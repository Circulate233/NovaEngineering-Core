package github.kasuminova.novaeng.mixin.ic2;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.integration.ic2.energy.IC2EnergyNetRecovery;
import github.kasuminova.novaeng.common.integration.ic2.energy.IC2EnergyNetRecovery.StaleNode;
import github.kasuminova.stellarcore.mixin.ic2.AccessorGridChange;
import ic2.api.energy.tile.IEnergyTile;
import ic2.core.energy.grid.EnergyNetLocal;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

@Mixin(targets = "ic2.core.energy.grid.ChangeHandler", remap = false)
public abstract class MixinEnergyNetChangeHandler {
    @Shadow
    static void applyRemoval(EnergyNetLocal enet, IEnergyTile ioTile, BlockPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "prepareSync", at = @At("RETURN"), cancellable = true, require = 1)
    private static void nova$prepare(EnergyNetLocal enet, @Coerce Object rawChange, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        AccessorGridChange change = (AccessorGridChange) rawChange;
        if (!IC2EnergyNetRecovery.prepare(enet, change.getIoTile(), change.getSubTiles()))
            cir.setReturnValue(Boolean.FALSE);
    }

    @Inject(method = "applyAddition", at = @At("HEAD"), require = 1)
    private static void nova$removeStale(EnergyNetLocal enet, IEnergyTile ioTile, BlockPos pos,
                                         List<IEnergyTile> subTiles, Collection<?> pendingChanges, CallbackInfo ci) {
        List<StaleNode> stale = IC2EnergyNetRecovery.takeRepairs(enet, ioTile);
        for (StaleNode node : stale) applyRemoval(enet, node.ioTile(), node.position());
        if (!stale.isEmpty()) {
            NovaEngineeringCore.log.warn("Recovered {} stale energy node(s) before registering {} at {}", stale.size(), ioTile, pos);
            IC2EnergyNetRecovery.notifyIntegrations(enet, ioTile, stale);
        }
    }
}
