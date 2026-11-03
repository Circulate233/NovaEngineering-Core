package github.kasuminova.novaeng.mixin.ar;

import github.kasuminova.novaeng.common.performance.ObjLineSyntax;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zmaster587.advancedRocketry.backwardCompat.WavefrontObject;

@Mixin(value = WavefrontObject.class, remap = false)
public abstract class MixinWavefrontSyntax {
    /**
     * @author circulation
     * @reason Recognize the original OBJ grammar without allocating or sharing Matchers.
     */
    @Overwrite
    private static boolean isValidVertexLine(final String line) {
        return ObjLineSyntax.numbers(line, "v", 3, 4);
    }

    /**
     * @author circulation
     * @reason Recognize the original OBJ grammar without allocating or sharing Matchers.
     */
    @Overwrite
    private static boolean isValidVertexNormalLine(final String line) {
        return ObjLineSyntax.numbers(line, "vn", 3, 4);
    }

    /**
     * @author circulation
     * @reason Recognize the original OBJ grammar without allocating or sharing Matchers.
     */
    @Overwrite
    private static boolean isValidTextureCoordinateLine(final String line) {
        return ObjLineSyntax.numbers(line, "vt", 2, 3);
    }

    /**
     * @author circulation
     * @reason Recognize the original OBJ grammar without allocating or sharing Matchers.
     */
    @Overwrite
    private static boolean isValidFace_V_VT_VN_Line(final String line) {
        return ObjLineSyntax.faceKind(line) == 3;
    }

    /**
     * @author circulation
     * @reason Recognize the original OBJ grammar without allocating or sharing Matchers.
     */
    @Overwrite
    private static boolean isValidFace_V_VT_Line(final String line) {
        return ObjLineSyntax.faceKind(line) == 2;
    }

    /**
     * @author circulation
     * @reason Recognize the original OBJ grammar without allocating or sharing Matchers.
     */
    @Overwrite
    private static boolean isValidFace_V_VN_Line(final String line) {
        return ObjLineSyntax.faceKind(line) == 4;
    }

    /**
     * @author circulation
     * @reason Recognize the original OBJ grammar without allocating or sharing Matchers.
     */
    @Overwrite
    private static boolean isValidFace_V_Line(final String line) {
        return ObjLineSyntax.faceKind(line) == 1;
    }

    /**
     * @author circulation
     * @reason Recognize the original OBJ grammar without allocating or sharing Matchers.
     */
    @Overwrite
    private static boolean isValidFaceLine(final String line) {
        return ObjLineSyntax.faceKind(line) != 0;
    }

    /**
     * @author circulation
     * @reason Recognize the original OBJ grammar without allocating or sharing Matchers.
     */
    @Overwrite
    private static boolean isValidGroupObjectLine(final String line) {
        return ObjLineSyntax.group(line);
    }

    @Redirect(method = "loadObjModel", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"), require = 1)
    private String nova$normalizeWhitespace(final String line, final String regex, final String replacement) {
        return ObjLineSyntax.collapseWhitespace(line);
    }

}
