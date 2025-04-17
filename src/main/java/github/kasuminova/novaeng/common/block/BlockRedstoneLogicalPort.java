package github.kasuminova.novaeng.common.block;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class BlockRedstoneLogicalPort extends Block{
    static final PropertyInteger POWER = PropertyInteger.create("power", 0, 15);
    public static BlockRedstoneLogicalPort INSTANCE = new BlockRedstoneLogicalPort();

    private BlockRedstoneLogicalPort() {
        super(Material.IRON);
        this.setHardness(2.0F);
        this.setResistance(10.0F);
        this.setSoundType(SoundType.METAL);
        this.setTranslationKey(NovaEngineeringCore.MOD_ID + '.' + "redstone_logical_port");
        this.setRegistryName(NovaEngineeringCore.MOD_ID, "redstone_logical_port");
        this.setHarvestLevel("pickaxe", 1);
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer.Builder(this).add(POWER).build();
    }

    @Override
    public @NotNull IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(POWER, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(POWER);
    }

    @Override
    public int getWeakPower(IBlockState blockState, @NotNull IBlockAccess blockAccess, @NotNull BlockPos pos, @NotNull EnumFacing side) {
        return blockState.getValue(POWER);
    }

    @Override
    public boolean canProvidePower(@NotNull IBlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(@NotNull IBlockState state, @NotNull IBlockAccess world, @NotNull BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    public int getStrongPower(@NotNull IBlockState blockState, @NotNull IBlockAccess blockAccess, @NotNull BlockPos pos, EnumFacing side) {
        final EnumFacing.Axis currentAxis = side.getAxis();
        if (currentAxis == EnumFacing.Axis.Y) return 0;

        final BlockPos neighborPos = pos.offset(side.getOpposite());
        final IBlockState neighborState = blockAccess.getBlockState(neighborPos);
        final Block neighborBlock = neighborState.getBlock();

        final boolean isComparator = (neighborBlock == Blocks.POWERED_COMPARATOR) || (neighborBlock == Blocks.UNPOWERED_COMPARATOR);
        if (!isComparator) return 0;

        final EnumFacing comparatorFacing = neighborState.getValue(BlockHorizontal.FACING);
        final boolean isOrthogonalAxis = (comparatorFacing.getAxis() != currentAxis);

        return isOrthogonalAxis ? blockState.getValue(POWER) : 0;
    }

}
