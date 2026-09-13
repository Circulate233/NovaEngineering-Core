package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.util.ClientLightingGuard;
import github.kasuminova.novaeng.client.util.LightingConstants;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.settings.GameSettings;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Redirect(method = "updateLightmap(F)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;gammaSetting:F", opcode = Opcodes.GETFIELD))
    private float nova$fullbrightGamma(final GameSettings settings) {
        return ClientLightingGuard.isActive() ? LightingConstants.MAX_GAMMA : settings.gammaSetting;
    }

    @Redirect(method = "updateLightmap(F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;updateDynamicTexture()V"))
    private static void nova$skipLightmapUpload(final DynamicTexture texture) {
        if (ClientLightingGuard.shouldUploadLightmap()) {
            texture.updateDynamicTexture();
        }
    }
}
