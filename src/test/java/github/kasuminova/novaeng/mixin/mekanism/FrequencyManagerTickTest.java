package github.kasuminova.novaeng.mixin.mekanism;

import com.sun.management.ThreadMXBean;
import github.kasuminova.novaeng.common.util.FrequencyManagerRegistryAccess;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import mekanism.api.Coord4D;
import mekanism.common.frequency.Frequency;
import mekanism.common.frequency.FrequencyManager;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrequencyManagerTickTest {
    private static final String MANAGER = "mekanism.common.frequency.FrequencyManager";
    private static final String MIXIN = MixinFrequencyManagerTick.class.getName();
    private static final String LIFECYCLE = MixinMekanismFrequencyManagerLifecycle.class.getName();

    static void main(final String[] args) throws Throwable {
        final FrequencyManagerTickTest test = new FrequencyManagerTickTest();
        test.emptyManagerReturnsBeforeCreatingAValuesView();
        test.nonemptyManagerUsesOriginalCoordinateBodyOnTheCallingThread();
        test.outerLoopRetainsLoadOnceAndVirtualSubclassDispatch();
        test.stopOnlyUnregistersManagersDiscardedByThePrivateMaps();
        test.emptyTicksEliminateTheOriginalStreamAllocation();
        System.out.println("FrequencyManager: 5 target-bytecode and lifecycle checks passed.");
    }

    @Test
    void emptyManagerReturnsBeforeCreatingAValuesView() throws Exception {
        final Fixture fixture = new Fixture();
        final Object manager = fixture.manager("empty");
        final Int2ObjectOpenHashMap<Frequency> empty = new Int2ObjectOpenHashMap<>() {
            @Override
            public ObjectCollection<Frequency> values() {
                throw new AssertionError("empty tick must not request the values view");
            }
        };
        fixture.field("frequencies").set(manager, empty);
        fixture.manager.getMethod("tickSelf", World.class).invoke(manager, new Object[]{null});
    }

    @Test
    void nonemptyManagerUsesOriginalCoordinateBodyOnTheCallingThread() throws Exception {
        final Fixture fixture = new Fixture();
        final Object manager = fixture.manager("frequencies");
        final Int2ObjectOpenHashMap<Frequency> frequencies = new Int2ObjectOpenHashMap<>();
        final Thread thread = Thread.currentThread();
        final int[] visited = {0};
        for (int i = 0; i < 100; i++) {
            final Frequency frequency = new Frequency("frequency" + i, new UUID(0, i));
            frequency.activeCoords = new AbstractSet<>() {
                @Override
                public Iterator<Coord4D> iterator() {
                    assertSame(thread, Thread.currentThread());
                    visited[0]++;
                    return Collections.emptyIterator();
                }

                @Override
                public int size() {
                    return 0;
                }
            };
            frequencies.put(i, frequency);
        }
        fixture.field("frequencies").set(manager, frequencies);
        fixture.manager.getMethod("tickSelf", World.class).invoke(manager, new Object[]{null});
        assertEquals(100, visited[0]);
    }

    @Test
    void outerLoopRetainsLoadOnceAndVirtualSubclassDispatch() throws Exception {
        final Fixture fixture = new Fixture();
        final Class<?> recording = fixture.loader.loadClass(RecordingManager.class.getName());
        final Object manager = recording.getConstructor().newInstance();
        for (int tick = 0; tick < 3; tick++) {
            fixture.manager.getMethod("tick", World.class).invoke(null, new Object[]{null});
        }
        assertEquals(1, recording.getField("loads").getInt(manager));
        assertEquals(3, recording.getField("ticks").getInt(manager));
        assertSame(Thread.currentThread(), recording.getField("thread").get(manager));
    }

    @Test
    void stopOnlyUnregistersManagersDiscardedByThePrivateMaps() throws Exception {
        final Fixture fixture = new Fixture();
        final Class<?> lifecycle = fixture.loader.loadClass(LIFECYCLE);
        final Object harness = fixture.loader.loadClass(LifecycleHarness.class.getName()).getConstructor().newInstance();
        final Object publicManager = fixture.manager("public");
        final Object privateManager = fixture.manager("private");
        final Object addonManager = fixture.manager("addon-private");
        final Object stillOwned = fixture.manager("still-owned");
        final Int2ObjectOpenHashMap<Frequency> savedFrequencies = new Int2ObjectOpenHashMap<>();
        savedFrequencies.put(1, new Frequency("saved-private", new UUID(0, 123)));
        fixture.field("frequencies").set(privateManager, savedFrequencies);
        final Class<?> handlerClass = fixture.loader.loadClass(MANAGER + "$FrequencyDataHandler");
        final Object saveHandler = handlerClass.getConstructor(String.class).newInstance("privateFrequencyHandler");
        handlerClass.getMethod("setManager", fixture.manager).invoke(saveHandler, privateManager);
        fixture.field("dataHandler").set(privateManager, saveHandler);
        final Object2ObjectOpenHashMap<UUID, Object> teleporters = new Object2ObjectOpenHashMap<>();
        final Object2ObjectOpenHashMap<UUID, Object> entangloporters = new Object2ObjectOpenHashMap<>();
        teleporters.put(new UUID(0, 1), privateManager);
        teleporters.put(new UUID(0, 2), publicManager);
        entangloporters.put(new UUID(0, 1), privateManager); // Identity duplicate must be harmless.
        entangloporters.put(new UUID(0, 3), stillOwned);
        lifecycle.getField("privateTeleporters").set(null, teleporters);
        lifecycle.getField("privateEntangloporters").set(null, entangloporters);
        lifecycle.getField("publicTeleporters").set(null, publicManager);
        fixture.lifecycle(lifecycle, harness, "nova$capturePrivateManagers");
        assertEquals(4, fixture.registry().size(), "capture must not change live save ownership");
        assertSame(saveHandler, fixture.field("dataHandler").get(privateManager));
        assertEquals(1, savedFrequencies.size(), "live private frequency data must stay available to saving");
        teleporters.clear();
        entangloporters.clear();
        entangloporters.put(new UUID(0, 3), stillOwned);
        fixture.manager.getMethod("reset").invoke(null);
        fixture.lifecycle(lifecycle, harness, "nova$releaseDiscardedPrivateManagers");
        assertFalse(fixture.registry().contains(privateManager));
        assertTrue(fixture.registry().contains(publicManager));
        assertTrue(fixture.registry().contains(addonManager), "unknown addon owners are retained");
        assertTrue(fixture.registry().contains(stillOwned));
        assertFalse(fixture.manager.getField("loaded").getBoolean(null));
        assertNull(fixture.field("dataHandler").get(privateManager));
        assertTrue(savedFrequencies.isEmpty(), "the original reset still runs before unregistering");
        // A later server can register the same private name/owner with no stale registry entry.
        final Object replacement = fixture.manager("private");
        assertTrue(fixture.registry().contains(replacement));
        assertEquals(4, fixture.registry().size());
        fixture.lifecycle(lifecycle, harness, "nova$releaseDiscardedPrivateManagers");
        assertEquals(4, fixture.registry().size(), "cleanup snapshot must be released after use");
    }

    @Test
    void emptyTicksEliminateTheOriginalStreamAllocation() throws Throwable {
        final Fixture original = new Fixture(false);
        final Fixture optimized = new Fixture();
        final Object originalManager = original.manager("empty");
        final Object optimizedManager = optimized.manager("empty");
        final MethodType signature = MethodType.methodType(void.class, Object.class, World.class);
        final MethodHandle originalTick = MethodHandles.publicLookup()
                                                       .unreflect(original.manager.getMethod("tickSelf", World.class)).asType(signature);
        final MethodHandle optimizedTick = MethodHandles.publicLookup()
                                                        .unreflect(optimized.manager.getMethod("tickSelf", World.class)).asType(signature);
        for (int i = 0; i < 100_000; i++) {
            originalTick.invokeExact(originalManager, (World) null);
            optimizedTick.invokeExact(optimizedManager, (World) null);
        }
        final ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        final long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 200_000; i++) originalTick.invokeExact(originalManager, (World) null);
        final long originalBytes = bean.getThreadAllocatedBytes(thread) - before;
        before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 200_000; i++) optimizedTick.invokeExact(optimizedManager, (World) null);
        final long optimizedBytes = bean.getThreadAllocatedBytes(thread) - before;
        assertTrue(originalBytes > 10_000_000);
        assertTrue(optimizedBytes < originalBytes / 100,
            "empty ticks must remove the stream/task allocation: " + originalBytes + " -> " + optimizedBytes);
        System.out.println("200000 empty tickSelf calls allocated " + originalBytes + " -> " + optimizedBytes + " bytes.");
    }

    /**
     * Loads the actual target and merges only the production replacements; the original lambda stays untouched.
     */
    private static final class Fixture {
        private final boolean optimized;
        private final ClassLoader loader = new ClassLoader(FrequencyManagerTickTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                if (!name.equals(MANAGER) && !name.startsWith(MANAGER + "$") && !name.equals(LIFECYCLE)
                    && !name.equals(RecordingManager.class.getName()) && !name.equals(LifecycleHarness.class.getName())) {
                    return super.loadClass(name, resolve);
                }
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    try {
                        final ClassNode original = read(name);
                        if (name.equals(MANAGER) && Fixture.this.optimized) {
                            final ClassNode mixin = read(MIXIN);
                            original.interfaces.add(FrequencyManagerRegistryAccess.class.getName().replace('.', '/'));
                            original.methods.removeIf(m -> m.name.equals("tick") || m.name.equals("tickSelf"));
                            for (final MethodNode method : mixin.methods) {
                                if (!method.name.equals("tick") && !method.name.equals("tickSelf")
                                    && !method.name.equals("nova$unregisterDiscardedManager")) continue;
                                method.visibleAnnotations = null;
                                method.invisibleAnnotations = null;
                                for (final var instruction : method.instructions) {
                                    if (instruction instanceof MethodInsnNode call && call.owner.equals(mixin.name))
                                        call.owner = original.name;
                                    if (instruction instanceof FieldInsnNode field && field.owner.equals(mixin.name))
                                        field.owner = original.name;
                                }
                                original.methods.add(method);
                            }
                        }
                        final ClassWriter out = new ClassWriter(0);
                        original.accept(out);
                        final byte[] bytes = out.toByteArray();
                        type = defineClass(name, bytes, 0, bytes.length);
                    } catch (final Exception failure) {
                        throw new ClassNotFoundException(name, failure);
                    }
                }
                if (resolve) resolveClass(type);
                return type;
            }

            private ClassNode read(final String name) throws Exception {
                try (var source = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    final ClassNode node = new ClassNode();
                    new ClassReader(source.readAllBytes()).accept(node, 0);
                    return node;
                }
            }
        };
        private final Class<?> manager;

        private Fixture() throws Exception {
            this(true);
        }

        private Fixture(final boolean optimized) throws Exception {
            this.optimized = optimized;
            this.manager = this.loader.loadClass(MANAGER);
        }

        private Object manager(final String name) throws Exception {
            return this.manager.getConstructor(Class.class, String.class, UUID.class)
                               .newInstance(Frequency.class, name, new UUID(0, 123));
        }

        private Field field(final String name) throws Exception {
            final Field field = this.manager.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }

        private Set<?> registry() throws Exception {
            return (Set<?>) field("managers").get(null);
        }

        private void lifecycle(final Class<?> type, final Object harness, final String name) throws Exception {
            final var method = type.getDeclaredMethod(name, FMLServerStoppingEvent.class, CallbackInfo.class);
            method.setAccessible(true);
            method.invoke(harness, null, new CallbackInfo("test", false));
        }
    }

    public static class LifecycleHarness extends MixinMekanismFrequencyManagerLifecycle {
        public LifecycleHarness() {
        }
    }

    public static class RecordingManager extends FrequencyManager {
        public int loads;
        public int ticks;
        public Thread thread;

        public RecordingManager() {
            super(Frequency.class, "recording");
        }

        @Override
        public void createOrLoad(final World world) {
            this.loads++;
        }

        @Override
        public void tickSelf(final World world) {
            this.thread = Thread.currentThread();
            this.ticks++;
        }
    }
}
