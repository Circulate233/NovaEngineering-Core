package github.kasuminova.novaeng.mixin.ae2;

import appeng.util.item.AEItemStack;
import github.kasuminova.novaeng.common.performance.AeOutputPreparation;
import github.kasuminova.novaeng.common.performance.AeOutputTarget;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "appeng.tile.inventory.AppEngNetworkInventory", remap = false)
public abstract class MixinNetworkInventoryOutputPreparation implements AeOutputTarget {
    @Redirect(method = "insertItem", at = @At(value = "INVOKE",
        target = "Lappeng/util/item/AEItemStack;fromItemStack(Lnet/minecraft/item/ItemStack;)Lappeng/util/item/AEItemStack;"), require = 1)
    private AEItemStack nova$reuseConversion(final ItemStack stack) {
        return AeOutputPreparation.convert(stack);
    }
}
