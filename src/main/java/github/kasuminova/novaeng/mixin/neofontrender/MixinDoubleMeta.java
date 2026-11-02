package github.kasuminova.novaeng.mixin.neofontrender;

import com.swabunga.spell.engine.DoubleMeta;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DoubleMeta.class, remap = false)
public abstract class MixinDoubleMeta {

    @Redirect(method = "transform", at = @At(value = "INVOKE",
        target = "Lcom/swabunga/spell/engine/DoubleMeta;stringAt(Ljava/lang/String;II[Ljava/lang/String;)Z"), require = 1)
    private static boolean nova$matchRegion(final String string, final int start, final int length,
                                            final String[] list) {
        if (start < 0 || start >= string.length() || list.length == 0) {
            return false;
        }
        final int end = start + length;
        if (end < start || end > string.length()) {
            throw new StringIndexOutOfBoundsException(
                "Range [" + start + ", " + end + ") out of bounds for length " + string.length());
        }
        for (final String candidate : list) {
            if (candidate.length() == length && string.regionMatches(start, candidate, 0, length)) {
                return true;
            }
        }
        return false;
    }
}
