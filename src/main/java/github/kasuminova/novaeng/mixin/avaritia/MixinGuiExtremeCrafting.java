package github.kasuminova.novaeng.mixin.avaritia;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import morph.avaritia.client.gui.GUIExtremeCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = GUIExtremeCrafting.class, remap = false)
public class MixinGuiExtremeCrafting {

    @ModifyArg(
        method = "drawGuiContainerForegroundLayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I",
            ordinal = 2
        ),
        index = 2
    )
    private int modifyDrawString(int y) {
        if (NovaEngCoreConfig.CLIENT.ExtremeCraftingUIModification) {
            return y - 35; // this.ySize - 96 + 2
        } else {
            return y;
        }
    }

}