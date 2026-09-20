package github.kasuminova.novaeng.common.block;

import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

/**
 * 原 Additions 模组 `novaextended` 两个矿石方块的重新实现。
 *
 * <p>数据包中没有指定 `harvest_tool` 之外的工具信息，原实现把 `harvest_level`
 * 直接映射到挖掘等级，这里通过覆写 {@link #getHarvestLevel} 保持一致。
 */
public class BlockExtendedOre extends Block {

    public BlockExtendedOre(final String name, final float hardness, final float resistance,
                            final int harvestLevel, final int lightLevel) {
        super(Material.ROCK);
        this.setHardness(hardness);
        this.setResistance(resistance);
        this.setSoundType(SoundType.STONE);
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setLightLevel(lightLevel / 15.0F);
        this.setHarvestLevel("pickaxe", harvestLevel);
        this.setDefaultState(this.blockState.getBaseState());
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, name));
        this.setTranslationKey(Tags.MOD_ID + '.' + name);
    }

    @Override
    public boolean canHarvestBlock(@NotNull final IBlockAccess world, @NotNull final BlockPos pos,
                                   @NotNull final EntityPlayer player) {
        return true;
    }

}
