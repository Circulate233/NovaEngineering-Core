package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.client.memory.OwnedAnimationFrames;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TextureAtlasSprite.class)
public abstract class MixinAnimationFrameStorage {
    @Shadow public List<int[][]> framesTextureData;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nova$rememberPrivateList(final String name, final CallbackInfo ci) {
        if (((Object) this).getClass() == TextureAtlasSprite.class) framesTextureData = new OwnedAnimationFrames();
    }

    @WrapOperation(method = "resetSprite", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;setFramesTextureData(Ljava/util/List;)V"), require = 1)
    private void nova$freshPrivateList(final TextureAtlasSprite sprite, final List<int[][]> frames, final Operation<Void> original) {
        original.call(sprite, ((Object) this).getClass() == TextureAtlasSprite.class ? new OwnedAnimationFrames() : frames);
    }

    @Inject(method = "generateMipmaps", at = @At("HEAD"))
    private void nova$stableMipmapInputs(final int level, final CallbackInfo ci) {
        if (framesTextureData instanceof OwnedAnimationFrames owned) owned.prepareMipmaps();
    }

    @WrapOperation(method = "generateMipmaps", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;setFramesTextureData(Ljava/util/List;)V"), require = 1)
    private void nova$ownedMipmapOutput(final TextureAtlasSprite sprite, final List<int[][]> frames, final Operation<Void> original) {
        original.call(sprite, framesTextureData instanceof OwnedAnimationFrames owned ? owned.mipmaps(frames) : frames);
    }

    @Redirect(method = {"loadSpriteFrames", "allocateFrameTextureData", "generateMipmaps", "updateAnimation", "updateAnimationInterpolated"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;framesTextureData:Ljava/util/List;", opcode = Opcodes.GETFIELD), require = 1)
    private List<int[][]> nova$borrowPrivateFrames(final TextureAtlasSprite sprite) {
        final List<int[][]> frames = sprite.framesTextureData;
        return frames instanceof OwnedAnimationFrames owned ? owned.internalView() : frames;
    }
}
