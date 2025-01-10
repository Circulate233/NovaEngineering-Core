package github.kasuminova.novaeng.mixin.ae2exttable;

import com._0xc4de.ae2exttable.client.gui.AE2ExtendedGUIs;
import com._0xc4de.ae2exttable.items.ItemRegistry;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemRegistry.class,remap = false)
public class MixinItemRegistry {
    @Inject(method = "guiByItem", at = @At(value = "HEAD"), cancellable = true)
    private static void onGuiByItem(Item item, CallbackInfoReturnable<AE2ExtendedGUIs> cir){
        if (item == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL){
            cir.setReturnValue(AE2ExtendedGUIs.WIRELESS_ULTIMATE_CRAFTING_TERMINAL);
        }
    }
}
