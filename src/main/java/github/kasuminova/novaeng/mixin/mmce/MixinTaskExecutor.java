package github.kasuminova.novaeng.mixin.mmce;

import com.google.common.collect.Sets;
import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import github.kasuminova.mmce.common.util.concurrent.ActionExecutor;
import github.kasuminova.mmce.common.util.concurrent.ExecuteGroup;
import github.kasuminova.novaeng.common.util.ITaskExecutor;
import github.kasuminova.novaeng.common.util.ReflectiveAccess;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;

@Mixin(value = TaskExecutor.class, remap = false)
public abstract class MixinTaskExecutor implements ITaskExecutor {

    @Unique
    private static final MethodHandle NOVA_GROUP_MAP = ReflectiveAccess.getter(Long2ObjectMaps.SynchronizedMap.class,
        "map", Long2ObjectMap.class);
    @Unique
    private static final VarHandle NOVA_GROUP_KEYS = ReflectiveAccess.field(Long2ObjectOpenHashMap.class, "key",
        long[].class);
    @Unique
    private static final VarHandle NOVA_GROUP_VALUES = ReflectiveAccess.field(Long2ObjectOpenHashMap.class, "value",
        Object[].class);
    @Unique
    private static final VarHandle NOVA_GROUP_SIZE = ReflectiveAccess.field(Long2ObjectOpenHashMap.class, "n", int.class);
    @Unique
    private static final VarHandle NOVA_GROUP_NULL_KEY = ReflectiveAccess.field(Long2ObjectOpenHashMap.class,
        "containsNullKey", boolean.class);
    @Shadow
    @Final
    private Queue<ActionExecutor> executors;
    @Shadow
    @Final
    private Queue<ActionExecutor> submitted;
    @Shadow
    @Final
    private Long2ObjectMap<ExecuteGroup> executeGroups;
    @Shadow
    @Final
    private Queue<ForkJoinTask<?>> forkJoinTasks;
    @Unique
    private LongArrayList nova$emptyExecuteGroups;
    @Unique
    private volatile Set<TileEntity> novaeng$requireMarkDirtyTEQueue;

    @Unique
    private static void nova$runGroup(final ExecuteGroup group) {
        ActionExecutor executor;
        while ((executor = group.poll()) != null) {
            executor.run();
        }
        group.setSubmitted(false);
    }

    @Unique
    private static Long2ObjectMap<ExecuteGroup> nova$groupMap(final Long2ObjectMap<ExecuteGroup> groups) {
        try {
            return (Long2ObjectMap<ExecuteGroup>) NOVA_GROUP_MAP.invoke(groups);
        } catch (Throwable exception) {
            throw new IllegalStateException("Unable to read Modular Machinery's execute group map", exception);
        }
    }

    @Shadow
    private void execute(final ActionExecutor executor) {
        throw new AssertionError();
    }

    @Inject(method = "updateTileEntity", at = @At("HEAD"), remap = false)
    private void injectUpdateTE(final CallbackInfo ci) {
        final Set<TileEntity> queue = novaeng$requireMarkDirtyTEQueue();
        synchronized (queue) {
            for (final TileEntity te : queue) {
                te.markDirty();
            }
            queue.clear();
        }
    }

    @Unique
    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public void addTEMarkTask(final TileEntity te) {
        final Set<TileEntity> queue = novaeng$requireMarkDirtyTEQueue();
        synchronized (queue) {
            queue.add(te);
        }
    }

    @Unique
    private Set<TileEntity> novaeng$requireMarkDirtyTEQueue() {
        Set<TileEntity> queue = novaeng$requireMarkDirtyTEQueue;
        if (queue == null) {
            synchronized (this) {
                queue = novaeng$requireMarkDirtyTEQueue;
                if (queue == null) {
                    queue = Sets.newIdentityHashSet();
                    novaeng$requireMarkDirtyTEQueue = queue;
                }
            }
        }
        return queue;
    }

    /**
     * @author NovaEngineering contributors
     * @reason Submit queued work and walk execute-group storage directly to avoid temporary entry allocations.
     */
    @Overwrite
    private synchronized void submitTask() {
        ActionExecutor executor;
        while ((executor = executors.poll()) != null) {
            execute(executor);
            submitted.offer(executor);
        }
        ForkJoinTask<?> forkJoinTask;
        while ((forkJoinTask = forkJoinTasks.poll()) != null) {
            TaskExecutor.FORK_JOIN_POOL.submit(forkJoinTask);
        }
        synchronized (executeGroups) {
            if (nova$emptyExecuteGroups == null) {
                nova$emptyExecuteGroups = new LongArrayList();
            }
            final Long2ObjectMap<ExecuteGroup> groups = nova$groupMap(executeGroups);
            final long[] keys = (long[]) NOVA_GROUP_KEYS.get(groups);
            final Object[] values = (Object[]) NOVA_GROUP_VALUES.get(groups);
            final int n = (int) NOVA_GROUP_SIZE.get(groups);
            nova$emptyExecuteGroups.clear();
            if ((boolean) NOVA_GROUP_NULL_KEY.get(groups)) {
                nova$submitGroup((ExecuteGroup) values[n]);
            }
            for (int index = n; index-- != 0; ) {
                if (keys[index] == 0L) {
                    continue;
                }
                nova$submitGroup((ExecuteGroup) values[index]);
            }
            for (int index = 0, size = nova$emptyExecuteGroups.size(); index < size; index++) {
                executeGroups.remove(nova$emptyExecuteGroups.getLong(index));
            }
        }
    }

    @Unique
    private void nova$submitGroup(final ExecuteGroup group) {
        if (group.isSubmitted()) {
            return;
        }
        if (group.isEmpty()) {
            nova$emptyExecuteGroups.add(group.getGroupId());
            return;
        }
        final ActionExecutor executor = new ActionExecutor(() -> nova$runGroup(group));
        group.setSubmitted(true);
        execute(executor);
        submitted.offer(executor);
    }

}
