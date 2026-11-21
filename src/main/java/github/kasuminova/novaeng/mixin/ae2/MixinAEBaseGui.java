package github.kasuminova.novaeng.mixin.ae2;

import appeng.client.gui.AEBaseGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AEBaseGui.class)
public class MixinAEBaseGui {

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", target = "Lappeng/util/Platform;isModLoaded(Ljava/lang/String;)Z", remap = false))
    private boolean hei(String modid) {
        return false;
    }
}
