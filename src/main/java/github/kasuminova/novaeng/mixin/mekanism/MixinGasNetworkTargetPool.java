package github.kasuminova.novaeng.mixin.mekanism;

import github.kasuminova.novaeng.common.util.NovaResettableTarget;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mekanism.api.gas.GasStack;
import mekanism.common.base.target.GasHandlerTarget;
import mekanism.common.transmitters.grid.GasNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * The gas network counterpart of {@link MixinEnergyNetworkTargetPool}: a {@code GasHandlerTarget} is built per
 * acceptor and per gas on every tick, and its constructor does nothing but store the gas it was built for.
 *
 * <p>Pooled instances are emptied and then given the gas the constructor argument names, so a reused instance is in
 * exactly the state a fresh one would have been in.</p>
 */
@Mixin(value = GasNetwork.class, remap = false)
public abstract class MixinGasNetworkTargetPool {

    @Unique
    private List<GasHandlerTarget> nova$targetPool;

    @Unique
    private int nova$poolIndex;

    @Inject(method = "collectTargets", at = @At("HEAD"), remap = false)
    private void nova$beginCollect(final CallbackInfo ci) {
        this.nova$poolIndex = 0;
    }

    @Redirect(method = "collectTargets", at = @At(value = "NEW",
        target = "mekanism/common/base/target/GasHandlerTarget"), remap = false)
    private GasHandlerTarget nova$obtainTarget(final GasStack gasStack) {
        List<GasHandlerTarget> pool = this.nova$targetPool;
        if (pool == null) {
            pool = new ObjectArrayList<>();
            this.nova$targetPool = pool;
        }

        final int index = this.nova$poolIndex++;
        final GasHandlerTarget target;
        if (index < pool.size()) {
            target = pool.get(index);
        } else {
            target = new GasHandlerTarget(gasStack);
            pool.add(target);
            return target;
        }
        final NovaResettableTarget resettable = (NovaResettableTarget) target;
        resettable.nova$reset();
        resettable.nova$setExtra(gasStack);
        return target;
    }
}
