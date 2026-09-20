package github.kasuminova.novaeng.mixin.avaritia;

import morph.avaritia.client.AvaritiaClientEventHandler;
import org.lwjgl.BufferUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.FloatBuffer;

@Mixin(value = AvaritiaClientEventHandler.class, remap = false)
public class MixinAvaritiaClientEventHandler {

    @Unique
    private static FloatBuffer nova$cachedUVBuffer = null;

    @Redirect(method = "onRenderTick", at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/BufferUtils;createFloatBuffer(I)Ljava/nio/FloatBuffer;"))
    private FloatBuffer nova$reuseUVBuffer(final int capacity) {
        if (nova$cachedUVBuffer == null || nova$cachedUVBuffer.capacity() < capacity) {
            nova$cachedUVBuffer = BufferUtils.createFloatBuffer(capacity);
        }
        nova$cachedUVBuffer.clear();
        return nova$cachedUVBuffer;
    }
}
