package github.kasuminova.novaeng.mixin.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

/**
 * Runs in the standard JUnit suite. Also accepts repository root and compiled main classes
 * directory (or main JAR) as main arguments. Never initializes Minecraft or optional mods.
 */
public final class MixinConfigIntegrityTest {
    private static final String ROOT = "github.kasuminova.novaeng.mixin.";
    private static final String DECISIONS = "github.kasuminova.novaeng.common.util.MixinDecisions";
    // Pre-existing unregistered implementations need separate review before activation.
    // Keep this exact: a newly added mixin must never become reachable merely via a decision string.
    private static final Map<String, String> DORMANT = Map.ofEntries(
        Map.entry("cofh.AccessorPacketHandler", "part of the inactive parallel packet sender"),
        Map.entry("cofh.MixinPacketHandler", "inactive parallel packet sender"),
        Map.entry("cofh.MixinTileCore", "part of the inactive parallel packet sender"),
        Map.entry("minecraft.forge.MixinFMLEventChannel", "inactive parallel packet sender"),
        Map.entry("minecraft.forge.MixinSimpleNetworkWrapper", "inactive parallel packet sender"),
        Map.entry("minecraft.forge.MixinSimpleIndexedCodec", "inactive packet profiler"),
        Map.entry("minecraft.forge.MixinNetworkDispatcherBandwidth", "inactive bandwidth protocol"),
        Map.entry("minecraft.forge.MixinFMLProxyPacket", "inactive packet exception logging"),
        Map.entry("minecraft.forge.MixinFMLServerHandler", "inactive server language override"),
        Map.entry("immersiveengineering.MixinExcavatorHandler", "inactive mineral generation behavior change"),
        Map.entry("minecraft.AccessorStitcherSlot", "accessor for inactive stitcher overwrite"),
        Map.entry("minecraft.MixinStitcher", "inactive stitcher overwrite"),
        Map.entry("minecraft.MixinTessellator", "no-op redirect with commented diagnostic code")
    );
    private static int checks;

    public static synchronized void main(final String[] args) throws Exception {
        checks = 0;
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected repository root and compiled main classes directory or JAR");
        }
        final Path repo = Path.of(args[0]).toAbsolutePath().normalize();
        final Path artifact = Path.of(args[1]).toAbsolutePath().normalize();
        require(Files.exists(artifact), "Missing compiled main artifact: " + artifact);
        final JsonObject config = JsonParser.parseString(Files.readString(
            repo.resolve("src/main/resources/novaeng_core.mixin.json"))).getAsJsonObject();
        require(config.get("package").getAsString().equals(ROOT.substring(0, ROOT.length() - 1)), "Mixin package");
        require(config.get("plugin").getAsString().equals("github.kasuminova.novaeng.NovaEngCoreMixinConfigPlugin"),
            "Audit the configured plugin's dynamic selection");

        final List<Path> files;
        try (final var paths = Files.walk(repo.resolve("src/main/java/github/kasuminova/novaeng/mixin"))) {
            files = paths.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        final Map<String, Boolean> types = new HashMap<>();
        for (final CompilationUnitTree unit : parse(files)) {
            final String prefix = unit.getPackageName().toString().substring(ROOT.length());
            new TreeScanner<Void, String>() {
                @Override
                public Void visitClass(final ClassTree tree, final String owner) {
                    final String name = (owner == null ? prefix + "." : owner + "$") + tree.getSimpleName();
                    final boolean mixin = tree.getModifiers().getAnnotations().stream().anyMatch(annotation ->
                        annotation.getAnnotationType().toString().equals("Mixin")
                            || annotation.getAnnotationType().toString().equals("org.spongepowered.asm.mixin.Mixin"));
                    types.put(name, mixin);
                    return super.visitClass(tree, name);
                }
            }.scan(unit, null);
        }
        final Map<String, String> registered = new LinkedHashMap<>();
        final List<String> problems = new ArrayList<>();
        try (final ZipFile jar = Files.isDirectory(artifact) ? null : new ZipFile(artifact.toFile())) {
            for (final String side : List.of("mixins", "client", "server")) {
                final JsonArray entries = config.getAsJsonArray(side);
                for (final JsonElement entry : entries) {
                    final String name = entry.getAsString();
                    if (registered.putIfAbsent(name, side) != null) problems.add("Duplicate registration: " + name);
                    if (!Boolean.TRUE.equals(types.get(name))) problems.add("Missing @Mixin source: " + name);
                    final String resource = (ROOT + name).replace('.', '/') + ".class";
                    if (!(jar == null ? Files.isRegularFile(artifact.resolve(resource)) : jar.getEntry(resource) != null)) {
                        problems.add("Missing compiled mixin: " + resource);
                    }
                    final String requiredSide = requiredSide(name);
                    if (requiredSide != null && !side.equals(requiredSide)) {
                        problems.add("Wrong side for " + name + ": " + side + ", expected " + requiredSide);
                    }
                    checks++;
                }
            }
        }
        types.forEach((name, mixin) -> {
            if (mixin && !registered.containsKey(name) && !DORMANT.containsKey(name)) {
                problems.add("Unregistered @Mixin (plugin must explicitly select it): " + name);
            }
        });
        for (final String name : DORMANT.keySet()) {
            require(Boolean.TRUE.equals(types.get(name)), "Stale dormant exception: " + name);
            require(!registered.containsKey(name), "Remove dormant exception after reviewing activation: " + name);
        }
        // The current plugin returns null. If it starts generating mixins, extend the audit to its
        // actual output instead of treating shouldApplyMixin as registration.
        final Path plugin = repo.resolve("src/main/java/github/kasuminova/novaeng/NovaEngCoreMixinConfigPlugin.java");
        boolean staticPlugin = false;
        for (final CompilationUnitTree unit : parse(List.of(plugin))) {
            final boolean[] found = {false};
            new TreeScanner<Void, Void>() {
                @Override
                public Void visitMethod(final MethodTree method, final Void unused) {
                    if (method.getName().contentEquals("getMixins")) {
                        found[0] = method.getBody().getStatements().size() == 1
                            && method.getBody().getStatements().getFirst().toString().equals("return null;");
                    }
                    return super.visitMethod(method, unused);
                }
            }.scan(unit, null);
            staticPlugin |= found[0];
        }
        require(staticPlugin, "Plugin dynamic selection changed; audit getMixins output explicitly");

        final Path decisions = repo.resolve("src/main/java/" + DECISIONS.replace('.', '/') + ".java");
        for (final CompilationUnitTree unit : parse(List.of(decisions))) {
            new TreeScanner<Void, Void>() {
                @Override
                public Void visitLiteral(final LiteralTree literal, final Void unused) {
                    if (literal.getValue() instanceof String name
                        && name.matches("[a-z][\\w.]*\\.(?:Mixin|Accessor|Invoker)[\\w$]+")
                        && !registered.containsKey(name)) {
                        problems.add("Decision names an unregistered or missing mixin: " + name);
                    }
                    return super.visitLiteral(literal, unused);
                }
            }.scan(unit, null);
        }
        checkDecisions(repo, decisions);
        System.out.println("Checked " + registered.size() + " compiled registrations; " + checks
            + " assertions; " + DORMANT.size() + " documented pre-existing dormant mixins.");
        require(problems.isEmpty(), String.join(System.lineSeparator(), problems));
        System.out.println("PASS: mixin configuration integrity and optional-mod/configuration gates");
    }

    private static String requiredSide(final String name) {
        if (name.startsWith("geometry.") || name.startsWith("moreoverlays.") || name.startsWith("actinium.")
            || Set.of("ftbquests.MixinQuestWrapper", "geckolib3.MixinRawGeometryTreeLookup",
            "mekanism.MixinSolarActivatorTexturePreload", "mekanism.MixinDimensionalStabilizerOverlaySnapshot",
            "astralsorcery.MixinBufferBatchCapacity", "neofontrender.MixinCosmicTessellatorCapacity",
            "neofontrender.MixinTextRouteCacheKeyHash", "neofontrender.MixinUiNavigationRuntime", "neofontrender.MixinUiTreeValidator",
            "minecraft.MixinGlobalTessellatorCapacity", "minecraft.MixinTesrTessellatorCapacity", "minecraft.MixinTextureUploadCapacity",
            "minecraft.forge.MixinSplashUploadBufferLifecycle", "customloadingscreen.MixinTextureAnimatorUploadMemory",
            "jei.MixinJeiStarterHashCacheCleanup", "jei.MixinRecipeRegistryIndexMemory", "jei.MixinHeiTableMemoryAccess",
            "enderutilities.MixinNullifierModelReloadLifecycle", "enderutilities.MixinNullifierModelBake").contains(name))
            return "client";
        if (Set.of("mekanism.MixinFrequencyManagerTick", "mekanism.MixinMekanismFrequencyManagerLifecycle",
            "minecraft.MixinLazyAttributeModifierMaps", "minecraft.MixinLazyPathHeap", "minecraft.MixinLazyPathFinder",
            "minecraft.MixinAttributeNameIndex", "minecraft.MixinAttributeNameIndexStorage",
            "enderio.MixinRecipesRegistration", "mmce.MixinBlockInformationIngredients",
            "ae2.MixinMECraftingInventorySnapshot", "mmce.MixinActiveRecipeLazyData", "mmce.MixinItemHandlerGenericCopy",
            "mmce.MixinItemUtilsGenericCopy", "mmce.InvokerItemHandlerCopyShellPool",
            "tconstruct.MixinTraitTagQueries", "tconstruct.MixinAbstractTraitTagQuery",
            "minecraft.forge.MixinChunkIOProviderNbt", "minecraft.forge.MixinFMLOutboundHandlerReply").contains(name))
            return "mixins";
        return null;
    }

    private static List<CompilationUnitTree> parse(final List<Path> paths) throws Exception {
        final var compiler = ToolProvider.getSystemJavaCompiler();
        require(compiler != null, "Run this test with a JDK");
        final var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (final var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            final JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                List.of("-proc:none", "-encoding", "UTF-8"), null, manager.getJavaFileObjectsFromPaths(paths));
            final List<CompilationUnitTree> result = new ArrayList<>();
            task.parse().forEach(result::add);
            require(diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR),
                "Source parse errors: " + diagnostics.getDiagnostics());
            return result;
        }
    }

    private static void checkDecisions(final Path repo, final Path decisions) throws Exception {
        final Path temp = Files.createTempDirectory("novaeng-mixin-config-test-");
        try {
            // Compile the real decision class against minimal API stubs. Derive boolean fields
            // from the real config source, so a misspelled setting still fails compilation.
            final StringBuilder config = new StringBuilder("package github.kasuminova.novaeng; public class NovaEngCoreConfig {");
            final Path configSource = repo.resolve("src/main/java/github/kasuminova/novaeng/NovaEngCoreConfig.java");
            for (final CompilationUnitTree unit : parse(List.of(configSource))) {
                new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitClass(final ClassTree tree, final Void unused) {
                        final String name = tree.getSimpleName().toString();
                        if (!Set.of("Client", "Server", "Performance").contains(name))
                            return super.visitClass(tree, unused);
                        config.append("public static ").append(name).append(' ').append(name.toUpperCase(Locale.ROOT))
                              .append(" = new ").append(name).append("(); public static class ").append(name).append(" {");
                        for (final var member : tree.getMembers()) {
                            if (member instanceof VariableTree field && field.getType().toString().equals("boolean")) {
                                config.append("public boolean ").append(field.getName()).append(" = true;");
                            }
                        }
                        config.append('}');
                        return null;
                    }
                }.scan(unit, null);
            }
            config.append('}');
            final Path stubConfig = temp.resolve("NovaEngCoreConfig.java");
            final Path stubDiscovery = temp.resolve("CleanroomModDiscoverer.java");
            Files.writeString(stubConfig, config);
            Files.writeString(stubDiscovery, """
                package com.cleanroommc.discovery;
                public final class CleanroomModDiscoverer {
                    public static final java.util.Set<String> MODS = new java.util.HashSet<>();
                    private static final CleanroomModDiscoverer INSTANCE = new CleanroomModDiscoverer();
                    public static CleanroomModDiscoverer instance() { return INSTANCE; }
                    public boolean isModPresent(String name) { return MODS.contains(name); }
                }
                """);
            final int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-proc:none", "-d", temp.toString(),
                decisions.toString(), stubConfig.toString(), stubDiscovery.toString());
            require(result == 0, "Compile actual MixinDecisions against the config API");
            final List<String> mods = List.of("actinium", "celeritasextra", "moreoverlays", "mekanism",
                "mekanismmultiblockmachine", "ftbquests", "itemfilters", "jei", "enderio", "geckolib3",
                "appliedenergistics2", "modularmachinery", "tconstruct", "astralsorcery", "neofontrender",
                "neofontrender_ui_enhancements", "customloadingscreen", "enderutilities");
            final List<Set<String>> scenarios = new ArrayList<>();
            scenarios.add(Set.of());
            scenarios.add(Set.copyOf(mods));
            for (final String omitted : mods) {
                final Set<String> present = new HashSet<>(mods);
                present.remove(omitted);
                scenarios.add(present);
            }
            final Path poolResource = temp.resolve("hellfirepvp/modularmachinery/common/util/ItemUtils$CopyShellPool.class");
            Files.createDirectories(poolResource.getParent());
            for (final boolean poolPresent : List.of(false, true)) {
                if (poolPresent) Files.write(poolResource, new byte[0]);
                else Files.deleteIfExists(poolResource);
                for (final Set<String> present : scenarios) {
                    try (final URLClassLoader loader = new URLClassLoader(new URL[]{temp.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                        final Class<?> discovery = loader.loadClass("com.cleanroommc.discovery.CleanroomModDiscoverer");
                        @SuppressWarnings("unchecked") final Set<String> loaded = (Set<String>) discovery.getField("MODS").get(null);
                        loaded.addAll(present);
                        final Class<?> actual = loader.loadClass(DECISIONS);
                        final Class<?> settings = loader.loadClass("github.kasuminova.novaeng.NovaEngCoreConfig");
                        for (final boolean enabled : List.of(false, true)) {
                            for (final String name : List.of("MixinGLStateManagerGenericAttributes",
                                "MixinGLSMCurrentAttribsGenericAttributes", "MixinRenderSystemGenericAttributes",
                                "MixinShaderManagerGenericAttributes")) {
                                gate(actual, settings, "CLIENT", "optimizeGenericAttributes", enabled,
                                    "actinium." + name, enabled);
                            }
                            for (final String name : List.of("MixinGLStateManagerVertexArray", "MixinGLSMVertexInputVertexArray",
                                "MixinGLSMAttribStackVertexArray", "MixinImmediateCommandListVertexArray",
                                "MixinPassThroughGLStateManagerVertexArray")) {
                                gate(actual, settings, "CLIENT", "optimizeVaoBindings", enabled,
                                    "actinium." + name, enabled);
                            }
                            gate(actual, settings, "CLIENT", "optimizeAtlasUploadBinding", enabled,
                                "actinium.MixinGLSMTexturesUploadBinding", enabled);
                            gate(actual, settings, "CLIENT", "optimizeAtlasUploadBinding", enabled,
                                "minecraft.MixinTextureMapUploadBinding", enabled && present.contains("actinium"));
                            for (final boolean startup : List.of(false, true)) {
                                gate(actual, settings, "PERFORMANCE", "optimizeStartupWork", startup,
                                    "minecraft.MixinDynamicTextureUploadBinding", enabled && startup && present.contains("actinium"));
                            }
                            gate(actual, settings, "CLIENT", "compressAnimationFrames", enabled,
                                "minecraft.MixinAnimationFrameStorage", enabled);
                            for (final String name : List.of("minecraft.MixinGlobalTessellatorCapacity", "minecraft.MixinTesrTessellatorCapacity",
                                "minecraft.MixinTextureUploadCapacity", "minecraft.forge.MixinSplashUploadBufferLifecycle")) {
                                gate(actual, settings, "CLIENT", "optimizeRetainedRenderBuffers", enabled, name, enabled);
                            }
                            for (final String name : List.of("MixinCosmicTessellatorCapacity")) {
                                gate(actual, settings, "CLIENT", "optimizeRetainedRenderBuffers", enabled, "neofontrender." + name,
                                    enabled && present.contains("neofontrender"));
                            }
                            gate(actual, settings, "CLIENT", "optimizeRetainedRenderBuffers", enabled, "astralsorcery.MixinBufferBatchCapacity",
                                enabled && present.contains("astralsorcery"));
                            gate(actual, settings, "CLIENT", "optimizeRetainedRenderBuffers", enabled, "customloadingscreen.MixinTextureAnimatorUploadMemory",
                                enabled && present.contains("customloadingscreen"));
                            for (final String name : List.of("MixinNullifierModelReloadLifecycle", "MixinNullifierModelBake")) {
                                gate(actual, settings, "CLIENT", "optimizeNullifierModel", enabled, "enderutilities." + name,
                                    enabled && present.contains("enderutilities"));
                            }
                            gate(actual, settings, "CLIENT", "optimizeMenuWorkingAllocations", enabled, "neofontrender.MixinTextRouteCacheKeyHash",
                                enabled && present.contains("neofontrender"));
                            for (final String name : List.of("MixinUiNavigationRuntime", "MixinUiTreeValidator")) {
                                gate(actual, settings, "CLIENT", "optimizeMenuWorkingAllocations", enabled, "neofontrender." + name,
                                    enabled && present.containsAll(List.of("neofontrender", "neofontrender_ui_enhancements")));
                            }
                            for (final String name : List.of("MixinJeiStarterHashCacheCleanup", "MixinRecipeRegistryIndexMemory", "MixinHeiTableMemoryAccess")) {
                                gate(actual, settings, "CLIENT", "optimizeJeiRecipeIndex", enabled, "jei." + name,
                                    enabled && present.contains("jei"));
                            }
                            gate(actual, settings, "CLIENT", "compactBakedGeometry", enabled, "geometry.block.MixinVanillaBakedModel", enabled);
                            gate(actual, settings, "CLIENT", "compactBakedGeometry", enabled, "geometry.actinium.AccessorGeometryTransformers", enabled && present.contains("actinium"));
                            gate(actual, settings, "CLIENT", "compactBakedGeometry", enabled, "geometry.MixinGeometryRenderItem", enabled && !present.contains("celeritasextra"));
                            gate(actual, settings, "CLIENT", "compactBakedGeometry", enabled, "geometry.MixinGeometryItemFrameLod", enabled && present.contains("celeritasextra"));
                            for (final String name : List.of("MixinLightOverlayRenderer", "MixinLightScannerBase")) {
                                gate(actual, settings, "CLIENT", "optimizeLightOverlay", enabled, "moreoverlays." + name, enabled && present.contains("moreoverlays"));
                            }
                            for (final String name : List.of("MixinFrequencyManagerTick", "MixinMekanismFrequencyManagerLifecycle")) {
                                gate(actual, settings, "SERVER", "optimizeFrequencyManagers", enabled, "mekanism." + name, enabled && present.contains("mekanism"));
                            }
                            for (final String name : List.of("MixinLazyAttributeModifierMaps", "MixinLazyPathHeap", "MixinLazyPathFinder", "MixinAttributeNameIndex", "MixinAttributeNameIndexStorage")) {
                                gate(actual, settings, "SERVER", "optimizeEntityConstruction", enabled, "minecraft." + name, enabled);
                            }
                            gate(actual, settings, "PERFORMANCE", "optimizeFerriteChunkNbt", enabled, "minecraft.forge.MixinChunkIOProviderNbt", enabled);
                            gate(actual, settings, "SERVER", "guardForgeNetworkReplyTarget", enabled, "minecraft.forge.MixinFMLOutboundHandlerReply", enabled);
                            for (final String name : List.of("MixinBlockArrayIngredients", "MixinBlockInformationIngredients")) {
                                gate(actual, settings, "CLIENT", "optimizeMmcePreviewIngredients", enabled, "mmce." + name, enabled);
                            }
                            gate(actual, settings, "PERFORMANCE", "optimizeStartupWork", enabled, "enderio.MixinRecipesRegistration", enabled && present.contains("enderio"));
                            gate(actual, settings, "PERFORMANCE", "optimizeStartupWork", enabled, "geckolib3.MixinRawGeometryTreeLookup", enabled && present.contains("geckolib3"));
                            gate(actual, settings, "PERFORMANCE", "optimizeStartupWork", enabled, "ftbquests.MixinQuestWrapper", enabled && present.containsAll(List.of("ftbquests", "itemfilters", "jei")));
                            gate(actual, settings, "PERFORMANCE", "optimizeStartupWork", enabled, "mekanism.MixinSolarActivatorTexturePreload", enabled && present.contains("mekanismmultiblockmachine"));
                            gate(actual, settings, "PERFORMANCE", "reduceRuntimeAllocations", enabled,
                                "ae2.MixinMECraftingInventorySnapshot", enabled && present.contains("appliedenergistics2"));
                            gate(actual, settings, "PERFORMANCE", "reduceRuntimeAllocations", enabled,
                                "mmce.MixinActiveRecipeLazyData", enabled && present.contains("modularmachinery"));
                            for (final String name : List.of("MixinItemHandlerGenericCopy", "MixinItemUtilsGenericCopy", "InvokerItemHandlerCopyShellPool")) {
                                gate(actual, settings, "PERFORMANCE", "reduceRuntimeAllocations", enabled,
                                    "mmce." + name, enabled && present.contains("modularmachinery") && poolPresent);
                            }
                            gate(actual, settings, "PERFORMANCE", "reduceRuntimeAllocations", enabled,
                                "mekanism.MixinDimensionalStabilizerOverlaySnapshot", enabled && present.contains("mekanism"));
                            for (final String name : List.of("MixinTraitTagQueries", "MixinAbstractTraitTagQuery")) {
                                gate(actual, settings, "PERFORMANCE", "reduceRuntimeAllocations", enabled,
                                    "tconstruct." + name, enabled && present.contains("tconstruct"));
                            }
                        }
                        gate(actual, settings, "CLIENT", "deferSolarActivatorScreen", false, "mekanism.MixinSolarActivatorTexturePreload", false);
                    }
                }
            }
        } finally {
            try (final var paths = Files.walk(temp)) {
                for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void gate(final Class<?> actual, final Class<?> config, final String group, final String setting,
                             final boolean value, final String name, final boolean expected) throws Exception {
        final Object category = config.getField(group).get(null);
        category.getClass().getField(setting).setBoolean(category, value);
        final boolean result = (boolean) actual.getMethod("shouldApply", String.class).invoke(null, name);
        require(result == expected, name + " with " + group + "." + setting + "=" + value + ": expected " + expected);
    }

    private static void require(final boolean condition, final String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    @Test
    void allMixinsAreRegisteredCompiledAndCorrectlyGated() throws Exception {
        final Path repo = Path.of("").toAbsolutePath().normalize();
        // Resolve the actual main output, including customized Gradle build directories,
        // without running MixinDecisions' static mod-discovery initialization.
        final Class<?> decisions = Class.forName(DECISIONS, false, getClass().getClassLoader());
        final Path artifact = Path.of(decisions.getProtectionDomain().getCodeSource().getLocation().toURI());
        main(new String[]{repo.toString(), artifact.toString()});
    }
}
