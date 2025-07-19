package github.kasuminova.novaeng.mixin.packagedauto;

import net.minecraft.client.gui.GuiButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thelm.packagedauto.client.gui.GuiContainerTileBase;
import thelm.packagedauto.client.gui.GuiUnpackager;
import thelm.packagedauto.container.ContainerUnpackager;

@Mixin(GuiUnpackager.class)
public abstract class MixinGuiUnpackager extends GuiContainerTileBase<ContainerUnpackager> {

    public MixinGuiUnpackager(ContainerUnpackager containerUnpackager) {
        super(containerUnpackager);
    }

    @Redirect(method = "initGui",at = @At(value = "INVOKE", target = "Lthelm/packagedauto/client/gui/GuiUnpackager;addButton(Lnet/minecraft/client/gui/GuiButton;)Lnet/minecraft/client/gui/GuiButton;"))
    public GuiButton initGui(GuiUnpackager instance, GuiButton guiButton) {
        if (guiButton.getClass().getSimpleName().equals("GuiButtonTracker")){
            return null;
        }
        return this.addButton(guiButton);
    }
}
