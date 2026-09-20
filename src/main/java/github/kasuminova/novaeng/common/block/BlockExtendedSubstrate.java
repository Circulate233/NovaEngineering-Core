package github.kasuminova.novaeng.common.block;

import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 原 Additions 模组 `novaextended-willowalloy_substrate` 的重新实现。
 *
 * <p>数据包声明 `bounding_box_max_y: 0.25` 与 `light_opacity: 14`，
 * 这里以自定义碰撞箱与不透明光衰减还原。
 */
public class BlockExtendedSubstrate extends Block {

    private static final AxisAlignedBB BOUNDS = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);

    public BlockExtendedSubstrate(final String name, final float hardness, final float resistance,
                                  final int harvestLevel) {
        super(Material.ROCK);
        this.setHardness(hardness);
        this.setResistance(resistance);
        this.setSoundType(SoundType.STONE);
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setLightOpacity(14);
        this.setHarvestLevel("pickaxe", harvestLevel);
        this.setDefaultState(this.blockState.getBaseState());
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, name));
        this.setTranslationKey(Tags.MOD_ID + '.' + name);
    }

    @Override
    @NotNull
    public AxisAlignedBB getBoundingBox(@NotNull final IBlockState state, @NotNull final IBlockAccess source,
                                        @NotNull final BlockPos pos) {
        return BOUNDS;
    }

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(@NotNull final IBlockState blockState,
                                                 @NotNull final IBlockAccess worldIn,
                                                 @NotNull final BlockPos pos) {
        return BOUNDS;
    }

    @Override
    public boolean isOpaqueCube(@NotNull final IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(@NotNull final IBlockState state) {
        return false;
    }

}
