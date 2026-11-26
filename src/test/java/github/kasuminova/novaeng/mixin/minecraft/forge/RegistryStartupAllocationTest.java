package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import net.minecraftforge.fml.common.ModContainer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryStartupAllocationTest {
    private static ModContainer container() {
        return (ModContainer) Proxy.newProxyInstance(ModContainer.class.getClassLoader(),
            new Class<?>[]{ModContainer.class}, (proxy, method, args) -> switch (method.getName()) {
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-container";
                default -> null;
            });
    }

    private static ClassNode read(final byte[] bytes) {
        final var node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static long calls(final MethodNode method, final String owner, final String name) {
        return Arrays.stream(method.instructions.toArray())
                               .filter(i -> i instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.contains(name)).count();
    }

    @Test
    void emptySyncCopiesAreSharedAndPopulatedCopiesStayIsolatedDuringReplay() throws Exception {
        final var mixin = new MixinForgeRegistrySyncAllocations() {
        };
        final Method copy = MixinForgeRegistrySyncAllocations.class.getDeclaredMethod(
            "nova$copyOnlyPopulatedOverrides", Iterable.class);
        copy.setAccessible(true);
        final var source = ArrayListMultimap.<String, Object>create();
        final Object first = new Object();
        final Object current = new Object();
        final Object callbackAdded = new Object();
        final Object empty = copy.invoke(mixin, source.get("absent"));
        assertSame(empty, copy.invoke(mixin, source.get("alsoAbsent")));
        assertTrue(((List<?>) empty).isEmpty());
        source.put("key", first);
        @SuppressWarnings("unchecked") final ArrayList<Object> snapshot =
            (ArrayList<Object>) copy.invoke(mixin, source.get("key"));
        snapshot.add(current); // sync appends the current winner before add callbacks.
        source.removeAll("key");
        source.put("key", callbackAdded);
        assertEquals(List.of(first, current), snapshot);
        assertEquals(List.of(callbackAdded), source.get("key"));
        assertTrue(((List<?>) empty).isEmpty());
    }

    @Test
    void ownerMissDoesNotPublishAnEntryAndHitsObserveCurrentOrder() throws Exception {
        final var mixin = new MixinLoadControllerOwnerLookup() {
        };
        final Method lookup = MixinLoadControllerOwnerLookup.class.getDeclaredMethod(
            "nova$skipAbsentOwnerList", ListMultimap.class, Object.class);
        lookup.setAccessible(true);
        final var owners = ArrayListMultimap.<String, ModContainer>create();
        final ModContainer first = container();
        final ModContainer second = container();
        final Object missing = lookup.invoke(mixin, owners, "example.pkg");
        assertTrue(((List<?>) missing).isEmpty());
        assertTrue(owners.isEmpty());
        owners.put("example.pkg", first);
        owners.put("example.pkg", second);
        final List<?> hit = (List<?>) lookup.invoke(mixin, owners, "example.pkg");
        assertSame(first, hit.getFirst());
        owners.removeAll("example.pkg");
        owners.put("example.pkg", second);
        assertSame(second, ((List<?>) lookup.invoke(mixin, owners, "example.pkg")).getFirst());
        assertSame(missing, lookup.invoke(mixin, owners, "different.pkg"));
    }

    @Test
    void actualForgeTargetsAcceptBothInjectorsAndKeepTheExistingWalk() throws Exception {
        final byte[][] transformed = IsolatedMixinTransformer.transformInPackage(
            "github.kasuminova.novaeng.mixin.minecraft.forge",
            new String[]{"net.minecraftforge.registries.ForgeRegistry", "net.minecraftforge.fml.common.LoadController"},
            "MixinForgeRegistrySyncAllocations", "MixinLoadControllerOwnerLookup");
        final var registry = read(transformed[0]);
        final var sync = registry.methods.stream().filter(m -> m.name.equals("sync")).findFirst().orElseThrow();
        assertEquals(1, calls(sync, registry.name, "nova$copyOnlyPopulatedOverrides"));
        assertEquals(1, calls(sync, "java/util/List", "isEmpty"));
        final var controller = read(transformed[1]);
        final var fallback = controller.methods.stream().filter(m -> m.name.equals("findActiveContainerFromStack"))
                                               .findFirst().orElseThrow();
        assertEquals(1, calls(fallback, "java/lang/StackWalker", "walk"));
        final var packageLookup = controller.methods.stream().filter(m -> m.name.equals("lambda$findActiveContainerFromStack$2"))
                                                    .findFirst().orElseThrow();
        assertEquals(1, calls(packageLookup, controller.name, "nova$skipAbsentOwnerList"));
    }
}
