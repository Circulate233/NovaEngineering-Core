package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.advancement.IndexedInventoryCriterion;
import github.kasuminova.novaeng.common.advancement.InventoryEvaluation;
import github.kasuminova.novaeng.common.advancement.ListenerInstanceSnapshot;
import github.kasuminova.novaeng.common.performance.PerformanceMetrics;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Walks the inventory_changed listeners from a snapshot of the listener set instead of the set itself.
 *
 * <p>{@code Listeners.trigger} iterates a {@code HashSet} of listeners and asks each one for its criterion instance
 * before testing it. Every trigger call therefore allocates an iterator and pays one virtual call to
 * {@code getCriterionInstance} per registered listener, on top of the {@code test} the listener was registered for. A
 * pack that registers the criterion from every mod it ships ends up with several hundred listeners on a single player,
 * and this runs once per player tick, so the walk itself costs more than the criteria it evaluates - in the profiled
 * pack 956 ms of a 40 s sample was spent inside {@code trigger}, against 128 ms inside all the {@code test} calls it
 * made.</p>
 *
 * <p>Both costs come from the collection, not from the criteria: the listener set is only ever added to and removed
 * from during registration, and a listener's criterion instance is final, so the set can be snapshotted into two
 * parallel arrays - the listeners, and the instances read out of them once. {@link ListenerInstanceSnapshot} holds
 * that pair; the walk here is an indexed loop over it. The instance array is what removes the per-listener
 * {@code getCriterionInstance} call, and the listener array keeps the grant pass that follows working on the same
 * objects in the same order.</p>
 *
 * <p>The snapshot is dropped by {@code add} and {@code remove} and rebuilt on the next trigger, so it is only ever
 * built from the current set. A listener granted during the grant pass removes itself from the set through
 * {@code remove}, which invalidates the snapshot while the walk that collected it is already finished - the walk
 * itself uses the array it took, so no iteration is affected by a change made during it. Nothing about which listeners
 * run, in what order they are tested, or which of them are granted changes: the set is still the source of truth and
 * is still what {@code isEmpty}, {@code add} and {@code remove} operate on.</p>
 *
 * <p>{@code trigger} is an overwrite and so has no runtime fallback; the mixin is instead gated at apply time by
 * {@code MixinDecisions}, the way the other overwrites in this codebase are.</p>
 */
@Mixin(targets = "net.minecraft.advancements.critereon.InventoryChangeTrigger$Listeners")
public abstract class MixinInventoryChangeTrigger$Listeners {

    @Shadow
    @Final
    private PlayerAdvancements playerAdvancements;

    /** The registered listeners, kept as the source of truth for the snapshot below. */
    @Shadow
    @Final
    private Set<ICriterionTrigger.Listener<InventoryChangeTrigger.Instance>> listeners;

    @Unique
    private final ListenerInstanceSnapshot<ICriterionTrigger.Listener<InventoryChangeTrigger.Instance>,
        InventoryChangeTrigger.Instance> nova$snapshot = new ListenerInstanceSnapshot<>();

    @Unique
    private final InventoryEvaluation nova$inventoryView = new InventoryEvaluation();

    /**
     * Drops the snapshot so the next trigger rebuilds it from the set the listener was just added to.
     *
     * @param listener the listener being registered
     * @param ci the injection callback
     */
    @Inject(method = "add", at = @At("TAIL"), require = 1)
    private void nova$invalidateOnAdd(final ICriterionTrigger.Listener<InventoryChangeTrigger.Instance> listener,
                                      final CallbackInfo ci) {
        this.nova$snapshot.invalidate();
    }

    /**
     * Drops the snapshot so the next trigger rebuilds it without the listener that was just removed.
     *
     * @param listener the listener being unregistered
     * @param ci the injection callback
     */
    @Inject(method = "remove", at = @At("TAIL"), require = 1)
    private void nova$invalidateOnRemove(final ICriterionTrigger.Listener<InventoryChangeTrigger.Instance> listener,
                                         final CallbackInfo ci) {
        this.nova$snapshot.invalidate();
    }

    /**
     * @author circulation
     * @reason Test the criteria from a snapshot of the listener set, so the walk allocates no iterator and reads each
     * listener's instance once instead of once per trigger.
     */
    @Overwrite
    public void trigger(final InventoryPlayer inventory) {
        final ListenerInstanceSnapshot<ICriterionTrigger.Listener<InventoryChangeTrigger.Instance>,
            InventoryChangeTrigger.Instance> snapshot = this.nova$snapshot;
        if (!snapshot.isHeld()) {
            snapshot.rebuild(this.listeners, ICriterionTrigger.Listener::getCriterionInstance);
        }

        final int size = snapshot.size();
        if (size == 0) {
            return;
        }
        ObjectArrayList<ICriterionTrigger.Listener<InventoryChangeTrigger.Instance>> matched = null;
        final boolean indexed = NovaEngCoreConfig.PERFORMANCE.sharedInventoryView;
        final InventoryEvaluation view = nova$inventoryView.inUse() ? new InventoryEvaluation() : nova$inventoryView;
        try {
            if (indexed) {
                boolean countFull = false;
                for (int i = 0; i < size && !countFull; i++) {
                    final InventoryChangeTrigger.Instance criterion = snapshot.instance(i);
                    if (criterion.getClass() == InventoryChangeTrigger.Instance.class) {
                        countFull = ((IndexedInventoryCriterion) criterion).nova$needsFullSlotCount();
                    }
                }
                view.prepare(inventory, countFull);
            }
            for (int index = 0; index < size; index++) {
                final InventoryChangeTrigger.Instance criterion = snapshot.instance(index);
                PerformanceMetrics.add(PerformanceMetrics.Counter.INVENTORY_CRITERIA, 1);
                final boolean accepted = indexed && criterion.getClass() == InventoryChangeTrigger.Instance.class
                    ? ((IndexedInventoryCriterion) criterion).nova$testIndexed(view)
                    : criterion.test(inventory);
                if (accepted) {
                    if (matched == null) {
                        matched = new ObjectArrayList<>(size);
                    }
                    matched.add(snapshot.listener(index));
                }
            }
        } finally {
            view.clear();
        }

        if (matched == null) {
            return;
        }
        final var z = matched.size();
        for (var i = 0; i < z; i++) {
            matched.get(i).grantCriterion(this.playerAdvancements);
        }
    }
}
