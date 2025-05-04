package github.kasuminova.novaeng.common.block;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import hellfirepvp.modularmachinery.common.block.BlockDynamicColor;
import hellfirepvp.modularmachinery.common.data.Config;
import hellfirepvp.modularmachinery.common.tiles.base.ColorableMachineTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class BlockRedstoneLogicalPort extends BlockContainer implements BlockDynamicColor {
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

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Nonnull
    @Override
    public EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public int getColorMultiplier(IBlockState state, @Nullable IBlockAccess worldIn, @Nullable BlockPos pos, int tintIndex) {
        if (worldIn == null || pos == null) {
            return Config.machineColor;
        }
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof ColorableMachineTile) {
            return ((ColorableMachineTile) te).getMachineColor();
        }
        return Config.machineColor;
    }

    @Override
    public boolean hasTileEntity(@NotNull IBlockState state) {
        return true;
    }

    @Override
    public boolean hasTileEntity() {
        return true;
    }

    @javax.annotation.Nullable
    @Override
    public TileEntity createTileEntity(@NotNull World world, @NotNull IBlockState state) {
        return new TileColorableMachineComponent();
    }

    @javax.annotation.Nullable
    @Override
    public TileEntity createNewTileEntity(@NotNull World worldIn, int meta) {
        return new TileColorableMachineComponent();
    }
}
