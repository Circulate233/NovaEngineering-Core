package github.kasuminova.novaeng.mixin.extrabotany;

import com.meteor.extrabotany.client.model.ModelGreenHat;
import com.meteor.extrabotany.client.render.CosmeticItemRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The supported model has fixed geometry and its render/rotation methods write no model state. */
@Mixin(value = CosmeticItemRenderLayer.class, remap = false)
public abstract class MixinCosmeticItemRenderLayerModels {
    @Unique
    private ModelGreenHat nova$greenHat;

    @Redirect(method = "greenHats", at = @At(value = "NEW",
        target = "com/meteor/extrabotany/client/model/ModelGreenHat"), require = 10, expect = 10)
    private ModelGreenHat nova$reuseGreenHat() {
        if (nova$greenHat == null) nova$greenHat = new ModelGreenHat();
        return nova$greenHat;
    }
}
