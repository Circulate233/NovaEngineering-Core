package github.kasuminova.novaeng.client.memory;

import github.kasuminova.mmce.client.renderer.BloomGeoModelRenderer;
import github.kasuminova.mmce.client.renderer.ControllerModelRenderManager;
import github.kasuminova.mmce.client.renderer.GeoModelRenderTask;
import github.kasuminova.mmce.client.renderer.MachineControllerRenderer;
import github.kasuminova.mmce.client.util.BufferBuilderPool;
import github.kasuminova.mmce.client.util.ReusableVBOUploader;
import github.kasuminova.novaeng.NovaEngineeringCore;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.world.World;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

/**
 * Runs between client frames; running model tasks keep their buffers until computation completes.
 */
@SuppressWarnings("unchecked")
public final class MmceRenderMemory {
    private static final VarHandle TASKS = field(MachineControllerRenderer.class, "tasks", Map.class);
    private static final VarHandle CONTROLLER = field(GeoModelRenderTask.class, "ctrl", TileMultiblockMachineController.class);
    private static final VarHandle CALLBACKS = field(ControllerModelRenderManager.class, "reinitializeCallback", Map.class);
    private static final VarHandle TO_RENDER = field(ControllerModelRenderManager.class, "toRender", Set.class);
    private static final VarHandle ALIVE = field(ControllerModelRenderManager.class, "alive", Set.class);
    private static final VarHandle DRAW_BUFFERS = field(ControllerModelRenderManager.class, "buffers", Map.class);
    private static final VarHandle UPLOADER = staticField(ControllerModelRenderManager.class, "VBO_UPLOADER", ReusableVBOUploader.class);
    private static final VarHandle UPLOAD_BUFFER = field(ReusableVBOUploader.class, "buffer", ByteBuffer.class);
    private static final VarHandle POOL = staticField(BufferBuilderPool.class, "POOL", NavigableMap.class);
    private static final VarHandle[] TASK_BUFFERS = taskBuffers();
    private static final MethodHandle CLEAN = cleaner("clean");
    private static final MethodHandle CLEAN_STATIC = cleaner("cleanStatic");
    private static final ObjectArrayList<GeoModelRenderTask> RETIRED = new ObjectArrayList<>();
    private static final ReferenceOpenHashSet<GeoModelRenderTask> WAITING = new ReferenceOpenHashSet<>();
    private static final ReferenceOpenHashSet<World> UNLOADED = new ReferenceOpenHashSet<>();

    private MmceRenderMemory() {
    }

    private static VarHandle field(final Class<?> type, final String name, final Class<?> valueType) {
        try {
            return MethodHandles.privateLookupIn(type, MethodHandles.lookup()).findVarHandle(type, name, valueType);
        } catch (final ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to access MMCE field " + name, error);
        }
    }

    private static VarHandle staticField(final Class<?> type, final String name, final Class<?> valueType) {
        try {
            return MethodHandles.privateLookupIn(type, MethodHandles.lookup()).findStaticVarHandle(type, name, valueType);
        } catch (final ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to access MMCE static field " + name, error);
        }
    }

    private static MethodHandle cleaner(final String name) {
        try {
            return MethodHandles.privateLookupIn(GeoModelRenderTask.class, MethodHandles.lookup())
                                .findVirtual(GeoModelRenderTask.class, name, MethodType.methodType(void.class));
        } catch (final ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to access MMCE buffer cleanup " + name, error);
        }
    }

    private static VarHandle[] taskBuffers() {
        final String[] names = {"buffer", "bloomBuffer", "transparentBuffer", "bloomTransparentBuffer",
            "staticBuffer", "staticBloomBuffer", "staticTransparentBuffer", "staticBloomTransparentBuffer"};
        final VarHandle[] fields = new VarHandle[names.length];
        for (int index = 0; index < names.length; index++)
            fields[index] = field(GeoModelRenderTask.class, names[index], BufferBuilder.class);
        return fields;
    }

    private static World world(final GeoModelRenderTask task) {
        return ((TileMultiblockMachineController) CONTROLLER.get(task)).getWorld();
    }

    public static void retire(final GeoModelRenderTask task) {
        // Cancellation alone does not prove that ForkJoinTask.compute() has stopped.
        if (task.isCancelled()) return;
        if (WAITING.add(task)) {
            RETIRED.add(task);
        }
    }

    public static void tick() {
        boolean released = false;
        for (int index = RETIRED.size() - 1; index >= 0; index--) {
            final GeoModelRenderTask task = RETIRED.get(index);
            if (!task.isDone()) continue;
            RETIRED.remove(index);
            WAITING.remove(task);
            final var controller = (TileMultiblockMachineController) CONTROLLER.get(task);
            final Map<TileMultiblockMachineController, GeoModelRenderTask> active =
                (Map<TileMultiblockMachineController, GeoModelRenderTask>) TASKS.get(MachineControllerRenderer.INSTANCE);
            if (active.get(controller) == task) continue;
            if (!task.isCancelled()) {
                ((Map<?, ?>) CALLBACKS.get(ControllerModelRenderManager.INSTANCE)).remove(task);
                final ReferenceOpenHashSet<BufferBuilder> obsolete = new ReferenceOpenHashSet<>();
                collectBuffers(task, obsolete);
                removeDrawBuffers(obsolete);
                release(task);
            }
            released = true;
        }
        if (RETIRED.isEmpty()) {
            UNLOADED.clear();
            if (released && Minecraft.getMinecraft().world == null) clearIdlePool();
        }
    }

    private static void discard(final GeoModelRenderTask task) {
        for (final VarHandle field : TASK_BUFFERS) field.setVolatile(task, null);
    }

    private static void collectBuffers(final GeoModelRenderTask task, final ReferenceOpenHashSet<BufferBuilder> target) {
        for (final VarHandle field : TASK_BUFFERS) {
            final BufferBuilder buffer = (BufferBuilder) field.getVolatile(task);
            if (buffer != null) target.add(buffer);
        }
    }

    private static void release(final GeoModelRenderTask task) {
        if (UNLOADED.contains(world(task)) || task.isCompletedAbnormally()) {
            discard(task);
            return;
        }
        try {
            CLEAN.invokeExact(task);
            CLEAN_STATIC.invokeExact(task);
        } catch (final Throwable error) {
            discard(task);
            NovaEngineeringCore.log.warn("Unable to recycle retired MMCE model buffers", error);
        }
    }

    public static void clearWorld(final World world) {
        UNLOADED.add(world);
        final var renderer = MachineControllerRenderer.INSTANCE;
        final Map<TileMultiblockMachineController, GeoModelRenderTask> tasks =
            (Map<TileMultiblockMachineController, GeoModelRenderTask>) TASKS.get(renderer);
        final ObjectArrayList<GeoModelRenderTask> removed = new ObjectArrayList<>();
        final ReferenceOpenHashSet<BufferBuilder> obsolete = new ReferenceOpenHashSet<>();
        final var iterator = tasks.entrySet().iterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            if (entry.getKey().getWorld() != world) continue;
            iterator.remove();
            final GeoModelRenderTask task = entry.getValue();
            removed.add(task);
            collectBuffers(task, obsolete);
            if (MachineControllerRenderer.shouldUseBloom())
                BloomGeoModelRenderer.INSTANCE.unregisterGlobal(entry.getKey());
        }
        final var manager = ControllerModelRenderManager.INSTANCE;
        ((Map<Object, ?>) CALLBACKS.get(manager)).keySet().removeIf(key -> key instanceof GeoModelRenderTask task && world(task) == world);
        ((Set<TileMultiblockMachineController>) TO_RENDER.get(manager)).removeIf(controller -> controller.getWorld() == world);
        ((Set<TileMultiblockMachineController>) ALIVE.get(manager)).removeIf(controller -> controller.getWorld() == world);
        removeDrawBuffers(obsolete);
        UPLOAD_BUFFER.set((ReusableVBOUploader) UPLOADER.get(), null);
        for (final GeoModelRenderTask task : removed) retire(task);
        clearIdlePool();
        if (RETIRED.isEmpty()) UNLOADED.clear();
    }

    private static void removeDrawBuffers(final ReferenceOpenHashSet<BufferBuilder> obsolete) {
        final Map<?, Map<?, Int2ObjectMap<List<BufferBuilder>>>> drawBuffers =
            (Map<?, Map<?, Int2ObjectMap<List<BufferBuilder>>>>) DRAW_BUFFERS.get(ControllerModelRenderManager.INSTANCE);
        final var types = drawBuffers.values().iterator();
        while (types.hasNext()) {
            final var textures = types.next();
            final var textureGroups = textures.values().iterator();
            while (textureGroups.hasNext()) {
                final var lightGroups = textureGroups.next();
                lightGroups.values().removeIf(buffers -> {
                    buffers.removeIf(obsolete::contains);
                    return buffers.isEmpty();
                });
                if (lightGroups.isEmpty()) textureGroups.remove();
            }
            if (textures.isEmpty()) types.remove();
        }
    }

    private static void clearIdlePool() {
        synchronized (BufferBuilderPool.class) {
            ((Map<?, ?>) POOL.get()).clear();
            MmceBufferPoolBudget.cleared();
        }
    }
}
