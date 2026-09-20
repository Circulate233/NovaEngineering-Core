package github.kasuminova.novaeng.common.item;

import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.common.entity.EntityExtendedArrow;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * 原 Additions 模组 `novaextended-fallen_star_arrow` 的重新实现。
 */
public class ItemExtendedArrow extends ItemArrow {

    public ItemExtendedArrow() {
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, "fallen_star_arrow"));
        this.setTranslationKey(Tags.MOD_ID + ".fallen_star_arrow");
    }

    @Override
    public boolean hasEffect(@NotNull final ItemStack stack) {
        return true;
    }

    @Override
    @NotNull
    public EntityArrow createArrow(@NotNull final World world, @NotNull final ItemStack stack,
                                   @NotNull final EntityLivingBase shooter) {
        return new EntityExtendedArrow(world, shooter, stack);
    }

    @Override
    public boolean isInfinite(@NotNull final ItemStack stack, @NotNull final ItemStack bow,
                              @NotNull final EntityPlayer player) {
        return super.isInfinite(stack, bow, player);
    }

}
