package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.string.StringCanon;
import net.minecraft.client.renderer.block.model.ModelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelBlock.class)
public abstract class MixinModelBlockTextureStrings {
    @Redirect(method = "resolveTextureName(Ljava/lang/String;Lnet/minecraft/client/renderer/block/model/ModelBlock$Bookkeep;)Ljava/lang/String;",
        at = @At(value = "INVOKE", target = "Ljava/lang/String;substring(I)Ljava/lang/String;"))
    private String nova$textureKey(final String value, final int start) {
        return StringCanon.tokens().slice(value, start, value.length());
    }

    @ModifyVariable(method = "resolveTextureName(Ljava/lang/String;)Ljava/lang/String;", at = @At("HEAD"), argsOnly = true)
    private String nova$textureReference(final String value) {
        return value.charAt(0) == '#' ? value : StringCanon.tokens().join("", '#', value);
    }
}
