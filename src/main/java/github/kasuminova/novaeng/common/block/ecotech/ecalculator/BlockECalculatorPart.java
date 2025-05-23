package github.kasuminova.novaeng.common.block.ecotech.ecalculator;

import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;

import javax.annotation.Nonnull;

@SuppressWarnings("deprecation")
public abstract class BlockECalculatorPart extends BlockContainer {

    protected BlockECalculatorPart(final Material materialIn) {
        super(materialIn);
        this.translucent = true;
        this.fullBlock = false;
        this.lightOpacity = 0;
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setHardness(20.0F);
        this.setResistance(2000.0F);
        this.setSoundType(SoundType.METAL);
        this.setHarvestLevel("pickaxe", 2);
    }

    @Override
    public boolean hasTileEntity() {
        return true;
    }

    @Override
    public boolean isOpaqueCube(@Nonnull final IBlockState state) {
        return false;
    }

    @Override
    public boolean canEntitySpawn(@Nonnull final IBlockState state, @Nonnull final Entity entityIn) {
        return false;
    }

    @Nonnull
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Nonnull
    public EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

}
