package github.kasuminova.novaeng.mixin.ic2;

import github.kasuminova.novaeng.common.integration.ic2.energy.EnergyNetRecoveryAccess;
import github.kasuminova.novaeng.common.integration.ic2.energy.IC2EnergyNetRecovery.StaleNode;
import ic2.api.energy.tile.IEnergyTile;
import ic2.core.energy.grid.EnergyNetLocal;
import ic2.core.energy.grid.Tile;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Map;

@Mixin(value = EnergyNetLocal.class, remap = false)
public abstract class MixinEnergyNetLocalRecovery implements EnergyNetRecoveryAccess {
    @Unique
    private final Map<IEnergyTile, List<StaleNode>> nova$repairs = new Reference2ObjectOpenHashMap<>();
    @Shadow
    @Final
    Map<BlockPos, Tile> registeredTiles;

    @Override
    public Map<BlockPos, Tile> nova$getRegisteredTiles() {
        return registeredTiles;
    }

    @Override
    public Map<IEnergyTile, List<StaleNode>> nova$getRepairs() {
        return nova$repairs;
    }
}
