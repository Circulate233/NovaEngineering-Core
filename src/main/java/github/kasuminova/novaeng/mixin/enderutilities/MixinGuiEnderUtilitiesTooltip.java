package github.kasuminova.novaeng.mixin.enderutilities;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.enderutilities.gui.client.GuiHandyBag;
import fi.dy.masa.enderutilities.gui.client.base.GuiEnderUtilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiEnderUtilities.class)
public class MixinGuiEnderUtilitiesTooltip {
    @WrapOperation(method = "drawScreen", at = @At(value = "INVOKE", remap = false,
        target = "Lfi/dy/masa/enderutilities/gui/client/base/GuiEnderUtilities;drawTooltips(II)V"), require = 1)
    private void nova$deferHandyBagTooltip(final GuiEnderUtilities gui, final int mouseX, final int mouseY,
                                          final Operation<Void> original) {
        // HandyBag draws the same tooltip again after updating its positions and potion
        // layer. Keep that final pass (including Forge events), not this earlier one.
        if (!(gui instanceof GuiHandyBag)) {
            original.call(gui, mouseX, mouseY);
        }
    }
}
