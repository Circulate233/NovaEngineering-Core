package github.kasuminova.gradle.remap;

import kotlin.Pair;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.BuildersKt;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.gradle.api.logging.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import xyz.wagyourtail.unimined.api.mapping.MappingsConfig;
import xyz.wagyourtail.unimined.api.minecraft.MinecraftConfig;
import xyz.wagyourtail.unimined.internal.mapping.extension.MixinRemapExtension;
import xyz.wagyourtail.unimined.mapping.Namespace;
import xyz.wagyourtail.unimined.util.TrLoggerFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class FallbackModRemap {

    private static final String CLASS_SUFFIX = ".class";
    private static final int ASM_API = Opcodes.ASM9;

    private FallbackModRemap() {
    }

    public static void remapAll(
            MinecraftConfig config,
            Logger logger,
            Map<File, File> jobs,
            File workDir
    ) throws IOException, InterruptedException {
        if (jobs.isEmpty()) {
            return;
        }

        Namespace fromNamespace = config.getMappings().checkedNs("mcp");
        Namespace toNamespace = config.getMcPatcher().getProdNamespace();
        IMappingProvider mappings = createMappings(
                config.getMappings(),
                fromNamespace,
                toNamespace
        );

        List<Path> baseClasspath = new ArrayList<>();
        for (File library : config.getMinecraftLibraries().getFiles()) {
            baseClasspath.add(library.toPath());
        }
        baseClasspath.add(config.getMinecraft(fromNamespace));

        Map<String, Path> classIndex = buildClassIndex(config, logger, baseClasspath);
        for (Map.Entry<File, File> job : jobs.entrySet()) {
            File inputJar = job.getKey();
            Set<String> needed = collectNeededClasses(inputJar, classIndex);
            Path neededJar = extractNeededClasses(classIndex, needed, workDir, inputJar);
            logger.lifecycle(
                    "[RuntimeTest] {} needs {} classes from the mod class index",
                    inputJar.getName(),
                    needed.size()
            );

            List<Path> classpath = new ArrayList<>();
            if (Boolean.getBoolean("novaeng.fallbackRemapFullIndex")) {
                classpath.addAll(new LinkedHashSet<>(classIndex.values()));
            } else {
                classpath.add(neededJar);
            }
            classpath.addAll(baseClasspath);
            remapJar(
                    config,
                    logger,
                    mappings,
                    inputJar,
                    job.getValue(),
                    classpath
            );
        }
    }

    private static void remapJar(
            MinecraftConfig config,
            Logger logger,
            IMappingProvider mappings,
            File inputJar,
            File outputJar,
            List<Path> classpath
    ) throws IOException {
        TinyRemapper.Builder builder = TinyRemapper
                .newRemapper(new TrLoggerFilter(logger))
                .withMappings(mappings)
                .skipLocalVariableMapping(true)
                .ignoreConflicts(true)
                .threads(2);

        MixinRemapExtension mixinExtension = new MixinRemapExtension(logger, true);
        mixinExtension.enableBaseMixin();
        mixinExtension.enableMixinExtra();
        mixinExtension.disableRefmap();
        builder.extension(mixinExtension);
        config.getMinecraftRemapper().getTinyRemapperConf().invoke(builder);

        TinyRemapper remapper = builder.build();
        InputTag inputTag = remapper.createInputTag();
        Path inputPath = inputJar.toPath();
        await(mixinExtension.readClassPath(remapper, classpath.toArray(new Path[0])));
        await(mixinExtension.readInput(remapper, inputTag, inputPath));
        apply(remapper, inputTag, inputPath, outputJar);
        remapper.finish();
        insertExtra(mixinExtension, inputTag, outputJar);
    }

    private static Map<String, Path> buildClassIndex(
            MinecraftConfig config,
            Logger logger,
            List<Path> wholesale
    ) throws IOException {
        Set<String> shadowed = new HashSet<>();
        for (Path path : wholesale) {
            collectClassNames(path, shadowed);
        }
        Map<String, Path> index = new HashMap<>();
        LinkedHashSet<Path> jars = new LinkedHashSet<>();
        for (File file : config.getSourceSet().getCompileClasspath().getFiles()) {
            Path path = file.toPath();
            if (!file.exists() || config.isMinecraftJar(path)) {
                continue;
            }
            jars.add(path);
        }
        for (Path jar : jars) {
            indexJar(index, jar, shadowed);
        }
        logger.lifecycle(
                "[RuntimeTest] Mod class index: {} jars, {} classes",
                jars.size(),
                index.size()
        );
        return index;
    }

    private static void collectClassNames(Path jar, Set<String> names) throws IOException {
        File file = jar.toFile();
        if (!file.isFile()) {
            return;
        }
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(CLASS_SUFFIX)) {
                    names.add(name.substring(0, name.length() - CLASS_SUFFIX.length()));
                }
            }
        }
    }

    private static void indexJar(Map<String, Path> index, Path jar, Set<String> shadowed)
            throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(CLASS_SUFFIX)) {
                    continue;
                }
                String className = name.substring(0, name.length() - CLASS_SUFFIX.length());
                if (shadowed.contains(className)) {
                    continue;
                }
                index.putIfAbsent(className, jar);
            }
        }
    }

    private static Set<String> collectNeededClasses(
            File inputJar,
            Map<String, Path> classIndex
    ) throws IOException {
        Set<String> referenced = new HashSet<>();
        CollectingRemapper collector = new CollectingRemapper(referenced);
        try (ZipFile zip = new ZipFile(inputJar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(CLASS_SUFFIX)) {
                    continue;
                }
                byte[] bytes;
                try (InputStream stream = zip.getInputStream(entry)) {
                    bytes = stream.readAllBytes();
                }
                ClassReader reader = new ClassReader(bytes);
                reader.accept(
                        new ClassRemapper(new PassThroughVisitor(), collector),
                        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES
                );
                reader.accept(
                        new TargetAnnotationCollector(collector),
                        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE
                );
            }
        }

        Set<String> needed = new LinkedHashSet<>();
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>(referenced);
        while (!queue.isEmpty()) {
            String name = queue.poll();
            if (name == null || name.isEmpty() || name.startsWith("[") || name.startsWith("java/")) {
                continue;
            }
            if (!visited.add(name) || !classIndex.containsKey(name)) {
                continue;
            }
            needed.add(name);
            byte[] bytes = readClass(classIndex.get(name), name);
            if (bytes == null) {
                continue;
            }
            ClassReader reader = new ClassReader(bytes);
            String superName = reader.getSuperName();
            if (superName != null) {
                queue.add(superName);
            }
            Collections.addAll(queue, reader.getInterfaces());
        }
        return needed;
    }

    private static Path extractNeededClasses(
            Map<String, Path> classIndex,
            Set<String> needed,
            File workDir,
            File inputJar
    ) throws IOException {
        workDir.mkdirs();
        Path target = new File(workDir, inputJar.getName() + "-classpath.jar").toPath();
        Path temporary = new File(workDir, target.getFileName() + ".tmp").toPath();
        Files.deleteIfExists(temporary);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(temporary))) {
            for (String name : needed) {
                byte[] bytes = readClass(classIndex.get(name), name);
                if (bytes == null) {
                    continue;
                }
                out.putNextEntry(new JarEntry(name + CLASS_SUFFIX));
                out.write(bytes, 0, bytes.length);
                out.closeEntry();
            }
            out.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            out.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        Files.deleteIfExists(target);
        Files.move(temporary, target);
        return target;
    }

    private static byte[] readClass(Path jar, String className) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry(className + CLASS_SUFFIX);
            if (entry == null) {
                return null;
            }
            try (InputStream stream = zip.getInputStream(entry)) {
                return stream.readAllBytes();
            }
        }
    }

    // getTRMappings 是 suspend 函数，用 runBlocking 桥接一次。
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IMappingProvider createMappings(
            MappingsConfig<?> mappingsConfig,
            Namespace fromNamespace,
            Namespace toNamespace
    ) throws InterruptedException {
        Pair<Namespace, Namespace> namespaces = new Pair<>(fromNamespace, toNamespace);
        Object trMappings = BuildersKt.runBlocking(
                EmptyCoroutineContext.INSTANCE,
                (scope, continuation) -> mappingsConfig.getTRMappings(
                        namespaces,
                        false,
                        (Continuation) continuation
                )
        );
        return acceptor -> ((Function1) trMappings).invoke(acceptor);
    }

    private static void await(Object future) {
        if (future instanceof CompletableFuture) {
            ((CompletableFuture<?>) future).join();
        }
    }

    private static void apply(
            TinyRemapper remapper,
            InputTag inputTag,
            Path input,
            File outputFile
    ) throws IOException {
        outputFile.getParentFile().mkdirs();
        try (OutputConsumerPath consumer = new OutputConsumerPath.Builder(outputFile.toPath()).build()) {
            consumer.addNonClassFiles(
                    input,
                    remapper,
                    Collections.<OutputConsumerPath.ResourceRemapper>emptyList()
            );
            remapper.apply(consumer, inputTag);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static void insertExtra(
            MixinRemapExtension mixinExtension,
            InputTag inputTag,
            File outputFile
    ) throws IOException {
        Map<String, String> environment = new HashMap<>();
        environment.put("mutable", "true");
        try (FileSystem fileSystem = FileSystems.newFileSystem(
                URI.create("jar:" + outputFile.toPath().toUri()),
                environment
        )) {
            mixinExtension.insertExtra(inputTag, fileSystem);
        }
    }

    // 返回 null 会让 ASM 跳过方法体，类型引用就收不到。
    private static final class PassThroughVisitor extends ClassVisitor {

        private PassThroughVisitor() {
            super(ASM_API);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return new AnnotationVisitor(ASM_API) {
            };
        }

        @Override
        public FieldVisitor visitField(
                int access,
                String name,
                String descriptor,
                String signature,
                Object value
        ) {
            return new FieldVisitor(ASM_API) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return new AnnotationVisitor(ASM_API) {
                    };
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions
        ) {
            return new MethodVisitor(ASM_API) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return new AnnotationVisitor(ASM_API) {
                    };
                }
            };
        }
    }

    private static final class CollectingRemapper extends Remapper {

        private final Set<String> referenced;

        private CollectingRemapper(Set<String> referenced) {
            this.referenced = referenced;
        }

        @Override
        public String map(String internalName) {
            collect(internalName);
            return internalName;
        }

        @Override
        public String mapType(String internalName) {
            collect(internalName);
            return internalName;
        }

        void collect(String value) {
            if (value == null) {
                return;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                return;
            }
            if (trimmed.startsWith("[")) {
                collect(trimmed.substring(trimmed.lastIndexOf('[') + 1));
                return;
            }
            if (trimmed.startsWith("L") && trimmed.indexOf(';') > 0) {
                int cursor = 0;
                while (cursor < trimmed.length()) {
                    int start = trimmed.indexOf('L', cursor);
                    int end = start < 0 ? -1 : trimmed.indexOf(';', start);
                    if (end < 0) {
                        break;
                    }
                    add(trimmed.substring(start + 1, end));
                    cursor = end + 1;
                }
                return;
            }
            String candidate = trimmed;
            int dot = candidate.indexOf('.');
            if (dot > 0) {
                candidate = candidate.substring(0, dot);
            }
            int parenthesis = candidate.indexOf('(');
            if (parenthesis > 0) {
                candidate = candidate.substring(0, parenthesis);
            }
            add(candidate);
        }

        private void add(String name) {
            if (!name.isEmpty()) {
                referenced.add(name);
            }
        }
    }

    private static final class TargetAnnotationCollector extends ClassVisitor {

        private final CollectingRemapper collector;

        private TargetAnnotationCollector(CollectingRemapper collector) {
            super(ASM_API);
            this.collector = collector;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return new TargetAnnotationVisitor(collector, super.visitAnnotation(descriptor, visible));
        }

        @Override
        public FieldVisitor visitField(
                int access,
                String name,
                String descriptor,
                String signature,
                Object value
        ) {
            FieldVisitor next = super.visitField(access, name, descriptor, signature, value);
            return new FieldVisitor(ASM_API, next) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return new TargetAnnotationVisitor(collector, super.visitAnnotation(descriptor, visible));
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions
        ) {
            MethodVisitor next = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(ASM_API, next) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return new TargetAnnotationVisitor(collector, super.visitAnnotation(descriptor, visible));
                }
            };
        }
    }

    private static final class TargetAnnotationVisitor extends AnnotationVisitor {

        private final CollectingRemapper collector;

        private TargetAnnotationVisitor(CollectingRemapper collector, AnnotationVisitor next) {
            super(ASM_API, next);
            this.collector = collector;
        }

        @Override
        public void visit(String name, Object value) {
            if (value instanceof String) {
                collector.collect((String) value);
            } else if (value instanceof Type) {
                Type type = (Type) value;
                collector.collect(
                        type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName()
                );
            } else if (value instanceof String[]) {
                // 枚举值是 {描述符, 常量名}
                collector.collect(((String[]) value)[0]);
            }
            super.visit(name, value);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            collector.collect(descriptor);
            return new TargetAnnotationVisitor(collector, super.visitAnnotation(name, descriptor));
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return new TargetAnnotationVisitor(collector, super.visitArray(name));
        }
    }

}
