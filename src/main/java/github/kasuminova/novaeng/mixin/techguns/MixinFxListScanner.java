package github.kasuminova.novaeng.mixin.techguns;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import techguns.client.particle.TGFX;

import java.util.Scanner;
import java.util.regex.Pattern;

@Mixin(value = TGFX.class, remap = false)
public abstract class MixinFxListScanner {
    @Unique
    private static final String nova$FX_DELIMITER = "(\\s*=\\s*)|\\s\\{|\\s*//.*|\\r\\n|\\s+";
    @Unique
    private static final Pattern nova$FX_PATTERN = Pattern.compile(nova$FX_DELIMITER);

    @Redirect(method = "loadFXListFile", at = @At(value = "INVOKE",
        target = "Ljava/util/Scanner;useDelimiter(Ljava/lang/String;)Ljava/util/Scanner;"), require = 1)
    private static Scanner nova$reuseFxDelimiter(final Scanner scanner, final String delimiter) {
        return nova$FX_DELIMITER.equals(delimiter) ? scanner.useDelimiter(nova$FX_PATTERN) : scanner.useDelimiter(delimiter);
    }

    @Redirect(method = "loadFXListFile", at = @At(value = "INVOKE",
        target = "Ljava/util/Scanner;hasNextLine()Z"), require = 3, allow = 3)
    private static boolean nova$checkNextToken(final Scanner scanner) {
        // All three line lookaheads are followed by a token check. The parser never reads the line.
        return scanner.hasNext();
    }
}
