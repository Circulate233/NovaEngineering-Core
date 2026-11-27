package github.kasuminova.novaeng.client.font;

import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TooltipBindingsTest {
    @Test
    void stringOnlySnapshotSeesReplacementNullsBlanksAndInPlaceChanges() {
        final TooltipBindings cache = new TooltipBindings();
        final List<String> defaults = List.of("default");
        // The external TOML API supplies an exact ArrayList, which is the only list type cached here.
        final List<String> values = new ArrayList<>(Arrays.asList(null, " ", " x=1 ", "\t", "y=2"));
        final List<String> first = cache.resolve(values, defaults);
        assertEquals(List.of(" x=1 ", "y=2"), first);
        assertSame(first, cache.resolve(values, defaults));
        assertThrows(UnsupportedOperationException.class, () -> first.add("z=3"));
        values.set(2, "changed");
        final List<String> changed = cache.resolve(values, defaults);
        assertEquals(List.of("changed", "y=2"), changed);
        assertNotSame(first, changed);
        assertEquals(List.of(" x=1 ", "y=2"), first, "old result remains an immutable snapshot");
        values.clear();
        assertSame(defaults, cache.resolve(values, defaults));
        final List<String> otherDefaults = List.of("other default");
        assertSame(otherDefaults, cache.resolve(values, otherDefaults));
        assertSame(defaults, cache.resolve(null, defaults));
    }

    @Test
    void arbitraryObjectsKeepBothToStringCallsAndOversizedInputsAreNotRetained() {
        final TooltipBindings cache = new TooltipBindings();
        final ObjectArrayList<Object> objects = new ObjectArrayList<>();
        objects.add(new Object() {
            int calls;
            @Override public String toString() { return "call-" + ++calls; }
        });
        assertEquals(List.of("call-2"), cache.resolve(objects, List.of()));
        assertEquals(List.of("call-4"), cache.resolve(objects, List.of()));
        final ObjectArrayList<String> values = new ObjectArrayList<>(List.of("x".repeat(32769)));
        assertNotSame(cache.resolve(values, List.of()), cache.resolve(values, List.of()));
        values.clear();
        for (int i = 0; i < 257; i++) values.add("x");
        assertNotSame(cache.resolve(values, List.of()), cache.resolve(values, List.of()));
    }

    @Test
    void installedConfigGetterTransformsAndReadsTheLiveListOnce() throws Exception {
        final byte[] bytes = IsolatedMixinTransformer.transformInPackage(
            "github.kasuminova.novaeng.mixin.neofontrender",
            new String[]{"neofontrender.core.config.NeofontrenderConfig"}, "MixinFontBindingsSnapshot")[0];
        final ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        final var getter = node.methods.stream().filter(m -> m.name.equals("brilliantTextBindings")).findFirst().orElseThrow();
        int get = 0;
        int resolve = 0;
        for (final var instruction : getter.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("get")) {
                    assertTrue(call.desc.startsWith("(Ljava/util/List;)"), "use pre-parsed path, not repeated String split");
                    get++;
                }
                if (call.owner.endsWith("/TooltipBindings") && call.name.equals("resolve")) resolve++;
            }
        }
        assertEquals(1, get);
        assertEquals(1, resolve);
    }
}
