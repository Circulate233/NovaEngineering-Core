package github.kasuminova.novaeng.mixin.mekanism;

import github.kasuminova.novaeng.common.util.NovaResettableTarget;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mekanism.common.base.target.EnergyAcceptorTarget;
import mekanism.common.transmitters.grid.EnergyNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Keeps the per-acceptor targets of an energy network and hands them out again, instead of allocating one per
 * acceptor per tick.
 *
 * <p>{@code collectTargets} clears the network's target set and then builds a fresh {@code EnergyAcceptorTarget} for
 * every acceptor it finds, each of which allocates two enum maps in its constructor. The set is already reused across
 * ticks, so the targets can be as well: they never outlive the tick they were built for and the network holds them
 * only until the next {@code collectTargets} clears the set.</p>
 *
 * <p>The pool is indexed by the order the targets are created in, so within one tick every pooled instance is handed
 * out at most once and no two entries of the set can be the same object. A network that loses acceptors keeps the
 * unused instances for later, which is bounded by the largest acceptor count that network ever had.</p>
 */
@Mixin(value = EnergyNetwork.class, remap = false)
public abstract class MixinEnergyNetworkTargetPool {

    @Unique
    private List<EnergyAcceptorTarget> nova$targetPool;

    @Unique
    private int nova$poolIndex;

    @Inject(method = "collectTargets", at = @At("HEAD"), remap = false)
    private void nova$beginCollect(final CallbackInfo ci) {
        this.nova$poolIndex = 0;
    }

    @Redirect(method = "collectTargets", at = @At(value = "NEW",
        target = "mekanism/common/base/target/EnergyAcceptorTarget"), remap = false)
    private EnergyAcceptorTarget nova$obtainTarget() {
        List<EnergyAcceptorTarget> pool = this.nova$targetPool;
        if (pool == null) {
            pool = new ObjectArrayList<>();
            this.nova$targetPool = pool;
        }

        final int index = this.nova$poolIndex++;
        final EnergyAcceptorTarget target;
        if (index < pool.size()) {
            target = pool.get(index);
        } else {
            target = new EnergyAcceptorTarget();
            pool.add(target);
        }
        ((NovaResettableTarget) target).nova$reset();
        return target;
    }
}
