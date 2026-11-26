package github.kasuminova.novaeng.mixin.mcjtylib;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import mcjty.lib.font.TrueTypeFont;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TrueTypeFont.class, remap = false)
public abstract class MixinTrueTypeFontPixels {
    // loadImage copies all four bytes before the next call. Keep the scratch array local to
    // that invocation, so concurrent/reentrant image loads never share mutable storage.
    @Redirect(method = "loadImage", at = @At(value = "INVOKE",
        target = "Lmcjty/lib/font/TrueTypeFont;intToByteArray(I)[B"), require = 1)
    private static byte[] nova$reusePixelBytes(final int pixel,
                                               @Share("nova$pixelBytes") final LocalRef<byte[]> scratch) {
        byte[] bytes = scratch.get();
        if (bytes == null) {
            bytes = new byte[4];
            scratch.set(bytes);
        }
        bytes[0] = (byte) (pixel >>> 24);
        bytes[1] = (byte) (pixel >>> 16);
        bytes[2] = (byte) (pixel >>> 8);
        bytes[3] = (byte) pixel;
        return bytes;
    }
}
