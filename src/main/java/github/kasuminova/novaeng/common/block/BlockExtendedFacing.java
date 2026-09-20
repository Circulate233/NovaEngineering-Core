package github.kasuminova.novaeng.common.block;

import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * 原 Additions 模组 `novaextended-potato_server` 的重新实现。
 *
 * <p>原数据包声明 `type: additions:facing`，朝向属性由模组在运行时生成，
 * 因此这里显式实现 {@code facing} 属性与对应的 blockstate 变体。
 */
public class BlockExtendedFacing extends Block {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public BlockExtendedFacing(final String name, final float hardness, final float resistance,
                               final int harvestLevel, final int lightLevel) {
        super(Material.IRON);
        this.setHardness(hardness);
        this.setResistance(resistance);
        this.setSoundType(SoundType.METAL);
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setLightLevel(lightLevel / 15.0F);
        this.setHarvestLevel("pickaxe", harvestLevel);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, name));
        this.setTranslationKey(Tags.MOD_ID + '.' + name);
    }

    @Override
    public void onBlockPlacedBy(@NotNull final World world, @NotNull final BlockPos pos,
                                @NotNull final IBlockState state, @NotNull final EntityLivingBase placer,
                                @NotNull final ItemStack stack) {
        world.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
    }

    @Override
    @NotNull
    public IBlockState getStateFromMeta(final int meta) {
        EnumFacing facing = EnumFacing.byHorizontalIndex(meta);
        return this.getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public int getMetaFromState(@NotNull final IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    @NotNull
    public IBlockState withRotation(@NotNull final IBlockState state, @NotNull final Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @NotNull
    public IBlockState withMirror(@NotNull final IBlockState state, @NotNull final Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    @NotNull
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

}
