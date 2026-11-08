package github.kasuminova.novaeng.mixin.patchouli;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vazkii.patchouli.client.book.BookContents;

@Mixin(value = BookContents.class, remap = false)
public abstract class MixinBookContentsLocale {
    @Redirect(method = "loadLocalizedJson", at = @At(value = "INVOKE",
        target = "Ljava/lang/String;replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"), require = 1)
    private String nova$literalLanguagePath(final String path, final String pattern, final String language) {
        // Keep regex replacement diagnostics for unusual language IDs containing replacement metacharacters.
        return "en_us".equals(pattern) && language.indexOf('$') < 0 && language.indexOf('\\') < 0
            ? path.replace("en_us", language) : path.replaceAll(pattern, language);
    }
}
