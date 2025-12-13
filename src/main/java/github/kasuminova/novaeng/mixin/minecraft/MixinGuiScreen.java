package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public class MixinGuiScreen {

    @Inject(method = "handleComponentHover", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;renderToolTip(Lnet/minecraft/item/ItemStack;II)V"))
    protected void handleComponentHover(ITextComponent component, int x, int y, CallbackInfo ci, @Local(ordinal = 0) ItemStack itemstack) {
        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(itemstack, x - 18, y);
    }
}