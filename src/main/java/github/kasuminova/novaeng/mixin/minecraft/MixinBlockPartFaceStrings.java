package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.string.StringCanon;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockPartFace.class)
public abstract class MixinBlockPartFaceStrings {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static String nova$shareTexture(final String value) {
        return StringCanon.tokens().canonicalize(value);
    }
}
