package github.kasuminova.novaeng.mixin.konkrete;

import de.keksuccino.konkrete.input.StringUtils;
import github.kasuminova.novaeng.common.util.FormatCodeConversion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = StringUtils.class, remap = false)
public abstract class MixinStringUtils {
    /**
     * @author NovaEngineering
     * @reason Avoid compiling 22 patterns on every FancyMenu text refresh.
     */
    @Overwrite
    public static String convertFormatCodes(final String input, final String oldPrefix, final String newPrefix) {
        return FormatCodeConversion.convert(input, oldPrefix, newPrefix);
    }
}
