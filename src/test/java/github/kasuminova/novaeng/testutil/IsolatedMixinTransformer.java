package github.kasuminova.novaeng.testutil;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.logging.LoggerAdapterDefault;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.IPropertyKey;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.service.clean.ICleanMixinService;
import org.spongepowered.asm.util.ReEntranceLock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Runs the real dependency injectors without changing the test JVM's global Mixin service. */
public final class IsolatedMixinTransformer {
    private IsolatedMixinTransformer() {
    }

    public static byte[][] transform(final String[] targets, final String... mixins) throws Exception {
        final var urls = new LinkedHashSet<URL>();
        for (ClassLoader loader = IsolatedMixinTransformer.class.getClassLoader(); loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urlLoader) urls.addAll(List.of(urlLoader.getURLs()));
        }
        // Gradle uses a URLClassLoader; also support direct IDE/JUnit console execution.
        for (final String path : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            urls.add(java.nio.file.Path.of(path).toUri().toURL());
        }
        try (final URLClassLoader isolated = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            final Class<?> runner = Class.forName(IsolatedMixinTransformer.class.getName(), true, isolated);
            try {
                return (byte[][]) runner.getMethod("run", String[].class, String[].class).invoke(null, targets, mixins);
            } catch (final java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() instanceof Exception exception) throw exception;
                throw (Error) e.getCause();
            }
        }
    }

    public static byte[][] run(final String[] targets, final String[] mixins) throws Exception {
        final ClassLoader loader = IsolatedMixinTransformer.class.getClassLoader();
        final var getInstance = MixinService.class.getDeclaredMethod("getInstance");
        getInstance.setAccessible(true);
        final Object owner = getInstance.invoke(null);
        final Map<IPropertyKey, Object> properties = new HashMap<>();
        final Map<String, IPropertyKey> keys = new HashMap<>();
        final IGlobalPropertyService propertyService = (IGlobalPropertyService) Proxy.newProxyInstance(loader,
            new Class<?>[]{IGlobalPropertyService.class}, (proxy, method, args) -> switch (method.getName()) {
                case "resolveKey" -> keys.computeIfAbsent((String) args[0], key -> new IPropertyKey() { });
                case "setProperty" -> { properties.put((IPropertyKey) args[0], args[1]); yield null; }
                case "getProperty", "getPropertyString" -> properties.getOrDefault(args[0], args.length == 2 ? args[1] : null);
                default -> throw new AssertionError(method);
            });
        final String config = "{\"required\":true,\"minVersion\":\"0.8.7\",\"compatibilityLevel\":\"JAVA_25\","
            + "\"package\":\"github.kasuminova.novaeng.mixin.mmce\",\"mixins\":["
            + String.join(",", java.util.Arrays.stream(mixins).map(s -> "\"" + s + "\"").toList()) + "]}";
        final IClassBytecodeProvider bytecode = (IClassBytecodeProvider) Proxy.newProxyInstance(loader,
            new Class<?>[]{IClassBytecodeProvider.class}, (proxy, method, args) -> {
                try (final InputStream stream = loader.getResourceAsStream(((String) args[0]).replace('.', '/') + ".class")) {
                    if (stream == null) throw new ClassNotFoundException((String) args[0]);
                    final var node = new ClassNode();
                    new ClassReader(stream).accept(node, args.length > 2 ? (int) args[2] : 0);
                    return node;
                }
            });
        final IClassProvider classes = (IClassProvider) Proxy.newProxyInstance(loader,
            new Class<?>[]{IClassProvider.class}, (proxy, method, args) -> {
                if (method.getName().equals("getClassPath")) return new URL[0];
                return Class.forName((String) args[0], args.length > 1 && (boolean) args[1], loader);
            });
        final var lock = new ReEntranceLock(1);
        final IMixinService service = (IMixinService) Proxy.newProxyInstance(loader,
            new Class<?>[]{IMixinService.class, ICleanMixinService.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getName" -> "isolated-test";
                case "isValid" -> true;
                case "getInitialPhase" -> MixinEnvironment.Phase.DEFAULT;
                case "getLogger" -> new LoggerAdapterDefault((String) args[0]);
                case "getClassProvider" -> classes;
                case "getBytecodeProvider" -> bytecode;
                case "getReEntranceLock" -> lock;
                case "getPlatformAgents", "getMixinContainers" -> List.of();
                case "getPrimaryContainer" -> new ContainerHandleVirtual("isolated-test");
                case "getSideName" -> "SERVER";
                case "getFeatureValidator" -> IFeatureValidator.ALLOW_ALL;
                case "getAdviceProvider" -> IAdviceProvider.GENERIC;
                case "getResourceAsStream" -> args[0].equals("nova-lazy-recipe-test.json")
                    ? new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8)) : loader.getResourceAsStream((String) args[0]);
                case "getResource" -> loader.getResource((String) args[0]);
                default -> null;
            });
        for (final var value : Map.of("service", service, "propertyService", propertyService).entrySet()) {
            final var field = MixinService.class.getDeclaredField(value.getKey());
            field.setAccessible(true);
            field.set(owner, value.getValue());
        }
        MixinBootstrap.init();
        final Class<?> transformerClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");
        final var constructor = transformerClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        final Object transformer = constructor.newInstance();
        MixinExtrasBootstrap.init();
        Mixins.addConfiguration("nova-lazy-recipe-test.json");
        final var transform = transformerClass.getMethod("transformClassBytes", String.class, String.class, byte[].class);
        final byte[][] result = new byte[targets.length][];
        for (int i = 0; i < targets.length; i++) {
            final String name = targets[i].replace('/', '.');
            try (final InputStream stream = loader.getResourceAsStream(targets[i].replace('.', '/') + ".class")) {
                if (stream == null) throw new ClassNotFoundException(name);
                result[i] = (byte[]) transform.invoke(transformer, name, name, stream.readAllBytes());
            }
        }
        return result;
    }
}
