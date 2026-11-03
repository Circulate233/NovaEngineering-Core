package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.mmce.client.util.ReusableVBOUploader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.nio.ByteBuffer;

@Mixin(value = ReusableVBOUploader.class, remap = false)
public abstract class MixinReusableVBOUploaderGrowth {
    @Shadow
    protected ByteBuffer buffer;

    @ModifyArg(method = "checkBufferSize", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/GLAllocation;createDirectByteBuffer(I)Ljava/nio/ByteBuffer;", remap = true), index = 0, require = 1)
    private int nova$growUploadBuffer(final int requested) {
        if (buffer == null || requested <= 0) return requested;
        return (int) Math.clamp(requested, buffer.capacity() + (long) buffer.capacity() / 2, Integer.MAX_VALUE);
    }
}
