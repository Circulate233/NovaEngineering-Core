package github.kasuminova.novaeng.common.integration.ic2.energy;

import github.kasuminova.novaeng.common.integration.ic2.energy.IC2EnergyNetRecovery.StaleNode;
import ic2.api.energy.tile.IEnergyTile;
import ic2.core.energy.grid.Tile;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;

public interface EnergyNetRecoveryAccess {
    Map<BlockPos, Tile> nova$getRegisteredTiles();

    Map<IEnergyTile, List<StaleNode>> nova$getRepairs();
}
