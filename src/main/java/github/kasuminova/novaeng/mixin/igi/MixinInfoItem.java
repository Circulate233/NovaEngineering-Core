package github.kasuminova.novaeng.mixin.igi;

import com.github.lunatrius.ingameinfo.InGameInfoCore;
import com.github.lunatrius.ingameinfo.client.gui.overlay.InfoItem;
import github.kasuminova.novaeng.mixin.util.IMixinInGameInfoCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InfoItem.class)
public abstract class MixinInfoItem {

    @Shadow(remap = false) public abstract void drawInfo();

    @Inject(method = "drawInfo", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectDrawInfo(final CallbackInfo ci) {
        IMixinInGameInfoCore instance = (IMixinInGameInfoCore) InGameInfoCore.INSTANCE;
        if (!instance.isPostDrawing()) {
            instance.addPostDrawAction(this::drawInfo);
            ci.cancel();
        }
    }

}
