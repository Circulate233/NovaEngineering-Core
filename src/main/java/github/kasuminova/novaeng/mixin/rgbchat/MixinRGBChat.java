package github.kasuminova.novaeng.mixin.rgbchat;

import com.fred.jianghun.Main;
import github.kasuminova.novaeng.client.gui.font.CachedRGBFontRenderer;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
@SuppressWarnings("MethodMayBeStatic")
public class MixinRGBChat {

    @Inject(method = "postInit", at = @At("HEAD"), cancellable = true, remap = false)
    public void onPostInit(final FMLPostInitializationEvent event, final CallbackInfo ci) {
        CachedRGBFontRenderer.overrideFontRenderer();
        ci.cancel();
    }

}
