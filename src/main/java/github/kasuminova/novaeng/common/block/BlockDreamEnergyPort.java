package github.kasuminova.novaeng.common.block;

import github.kasuminova.novaeng.common.tile.TileDreamEnergyPort;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import sonar.fluxnetworks.common.block.BlockFluxStorage;

import javax.annotation.Nullable;

public class BlockDreamEnergyPort extends BlockFluxStorage {
    public static final BlockDreamEnergyPort INSTANCE = new BlockDreamEnergyPort();

    public BlockDreamEnergyPort() {
        super("DreamEnergyPort");
    }

    @Nullable
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileDreamEnergyPort();
    }

    public int getMaxStorage() {
        return Integer.MAX_VALUE;
    }
}
