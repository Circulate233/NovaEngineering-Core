package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real Mixin target validation: no target class initialization, game launch, or live JVM attachment. */
class RuntimeMachineMixinCompatibilityTest {
    private static final String MIXINS = "github.kasuminova.novaeng.mixin";
    private static final String PERF = "github/kasuminova/novaeng/common/performance/";
    private static final String PART = "appeng/api/util/AEPartLocation";
    private static final String ROTATION = "appeng/client/render/FacingToRotation";
    private static final List<Target> TARGETS = List.of(
        new Target("nc/tile/processor/IProcessor", "nuclearcraft.MixinProcessorAutoPushDirections",
            "nuclearcraft-overhauled-336895-6605808.jar", "autoPush"),
        new Target(PART, "ae2.MixinAEPartLocationLookup", "ae2-uel-v0.56.8-novaeng_ver.jar", "fromFacing"),
        new Target("appeng/parts/CableBusContainer", "ae2.MixinCableBusLightValues",
            "ae2-uel-v0.56.8-novaeng_ver.jar", "getLightValue"),
        new Target(ROTATION, "ae2.MixinFacingToRotationLookup", "ae2-uel-v0.56.8-novaeng_ver.jar", "get"),
        new Target("com/brandon3055/draconicevolution/blocks/tileentity/TileFusionCraftingCore",
            "draconicevolution.MixinFusionInjectorScan", "draconic-evolution-223565-3431261.jar", "updateInjectors"));

    @TestFactory
    Stream<DynamicTest> configuredDependenciesAcceptEveryMachineMixin() {
        return TARGETS.stream().map(target -> DynamicTest.dynamicTest("classpath " + target.mixin,
            () -> verify(target, Map.of())));
    }

    @TestFactory
    Stream<DynamicTest> installedProductionTargetsAcceptEveryMachineMixin() throws Exception {
        // Optional external fixture: CI still checks configured dependencies above. The local baseline
        // client is the default fixture; another installation can be supplied without editing tests.
        final Path mods = Path.of(System.getProperty("novaeng.test.runtimeMods",
            "F:/nova/NovaEngineering-World-V2/run/client/mods"));
        if (!Files.isDirectory(mods)) return Stream.empty();
        final Map<String, Map<String, byte[]>> jars = new HashMap<>();
        for (final Target target : TARGETS) if (!jars.containsKey(target.jar)) {
            final Path file = mods.resolve(target.jar);
            assertTrue(Files.isRegularFile(file), "Missing production fixture " + file);
            // Include inherited members and sibling classes from the same real JAR. Mixing a raw
            // target with dependency-version superclass metadata would miss compatibility failures.
            final Map<String, byte[]> classes = new HashMap<>();
            try (final ZipFile zip = new ZipFile(file.toFile())) {
                final var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    final var entry = entries.nextElement();
                    if (!entry.getName().endsWith(".class") || entry.getName().startsWith("META-INF/")) continue;
                    try (final var input = zip.getInputStream(entry)) {
                        classes.put(entry.getName().substring(0, entry.getName().length() - 6), input.readAllBytes());
                    }
                }
            }
            jars.put(target.jar, Map.copyOf(classes));
        }
        return TARGETS.stream().map(target -> DynamicTest.dynamicTest("production " + target.mixin,
            () -> verify(target, jars.get(target.jar))));
    }

    private static void verify(final Target target, final Map<String, byte[]> fixture) throws Exception {
        final byte[] original;
        if (fixture.containsKey(target.name)) original = fixture.get(target.name);
        else try (final var input = RuntimeMachineMixinCompatibilityTest.class.getClassLoader()
                                                                         .getResourceAsStream(target.name + ".class")) {
            assertNotNull(input, target.name);
            original = input.readAllBytes();
        }
        final ClassNode before = read(original);
        final Map<String, byte[]> overrides = new HashMap<>(fixture);
        if (target.method.equals("updateInjectors")) {
            // The real plugin runs this guarded patch in preApply for this marker. Perform the same
            // operation before isolated Mixin application without loading the production plugin.
            assertTrue(FusionInjectorScanTransform.apply(before), target.jar);
            final ClassWriter writer = new ClassWriter(0);
            before.accept(writer);
            overrides.put(target.name, writer.toByteArray());
        }
        final byte[] transformed = IsolatedMixinTransformer.transformInPackage(MIXINS,
            new String[]{target.name}, overrides, target.mixin)[0];
        assertFalse(Arrays.equals(original, transformed), target.mixin);
        final ClassNode after = read(transformed);
        final MethodNode changed = method(after, target.method);
        new Analyzer<>(new BasicInterpreter()).analyze(after.name, changed);
        switch (target.method) {
            case "autoPush" -> {
                assertTrue((after.access & Opcodes.ACC_INTERFACE) != 0);
                assertTrue((changed.access & Opcodes.ACC_PUBLIC) != 0);
                assertEquals(0, changed.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC));
                assertEquals(1, calls(changed, PERF + "ProcessorDirections", "snapshot"));
                assertEquals(1, calls(changed, target.name, "getAdjacentHandlers"));
                assertEquals(1, calls(changed, target.name, "autoPushInternal"));
                assertEquals("()Z", changed.desc);
            }
            case "fromFacing" -> {
                assertEquals("(Lnet/minecraft/util/EnumFacing;)L" + PART + ";", changed.desc);
                assertEquals(0, calls(changed, PART, "values"));
                assertLookupUsesSyntheticValues(after);
                assertValuesStillClones(after);
            }
            case "get" -> {
                assertEquals("(Lnet/minecraft/util/EnumFacing;Lnet/minecraft/util/EnumFacing;)L" + ROTATION + ";", changed.desc);
                assertEquals(0, calls(changed, ROTATION, "values"));
                assertLookupUsesSyntheticValues(after);
                assertValuesStillClones(after);
            }
            case "getLightValue" -> {
                assertEquals("()I", changed.desc);
                assertEquals(0, calls(changed, PART, "values"));
                assertTrue(after.fields.stream().anyMatch(field -> field.name.contains("nova$lightLocations")
                    && field.desc.equals("[L" + PART + ";")
                    && (field.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL))
                        == (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)));
                final ClassNode originalType = read(original);
                final int originalValuesCalls = originalType.methods.stream().filter(method -> method.name.equals("<clinit>"))
                    .mapToInt(method -> calls(method, PART, "values")).sum();
                assertEquals(originalValuesCalls + 1, calls(method(after, "<clinit>"), PART, "values"));
            }
            case "updateInjectors" -> {
                assertEquals(1, calls(changed, PERF + "FusionInjectorPositions", "scan"));
                assertEquals(1, calls(changed, PERF + "FusionInjectorPositions", "view"));
                assertEquals(0, calls(changed, "com/google/common/collect/Lists", "newArrayList"));
            }
            default -> throw new AssertionError(target);
        }
    }

    private static void assertLookupUsesSyntheticValues(final ClassNode node) {
        final MethodNode lookup = node.methods.stream().filter(method -> method.name.contains("nova$lookupValues"))
                                             .findFirst().orElseThrow();
        assertTrue(hasFieldRead(lookup, "$VALUES"));
    }

    private static void assertValuesStillClones(final ClassNode node) {
        int clones = 0;
        for (final var instruction : method(node, "values").instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("clone")) clones++;
        }
        assertEquals(1, clones, "Public enum values() must retain its defensive copy");
    }

    private static boolean hasFieldRead(final MethodNode method, final String name) {
        for (final var instruction : method.instructions) if (instruction instanceof FieldInsnNode field
            && field.name.equals(name) && (field.getOpcode() == Opcodes.GETFIELD || field.getOpcode() == Opcodes.GETSTATIC)) return true;
        return false;
    }

    private static int calls(final MethodNode method, final String owner, final String name) {
        int count = 0;
        for (final var instruction : method.instructions) if (instruction instanceof MethodInsnNode call
            && call.owner.equals(owner) && call.name.equals(name)) count++;
        return count;
    }

    private static ClassNode read(final byte[] bytes) {
        final ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static MethodNode method(final ClassNode node, final String name) {
        return node.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }

    private record Target(String name, String mixin, String jar, String method) {
    }
}
