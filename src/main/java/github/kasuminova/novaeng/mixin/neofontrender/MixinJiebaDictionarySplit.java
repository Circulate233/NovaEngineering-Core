package github.kasuminova.novaeng.mixin.neofontrender;

import com.huaban.analysis.jieba.WordDictionary;
import github.kasuminova.novaeng.common.performance.DictionaryLineSplitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Splits the dictionary's fixed tab/space delimiter without regex matchers or temporary lists.
 */
@Mixin(value = WordDictionary.class, remap = false)
public abstract class MixinJiebaDictionarySplit {

    @Redirect(method = {"loadDict()V", "loadUserDict(Ljava/nio/file/Path;Ljava/nio/charset/Charset;)V"},
        at = @At(value = "INVOKE", target = "Ljava/lang/String;split(Ljava/lang/String;)[Ljava/lang/String;"),
        require = 1)
    private String[] nova$reuseDelimiterPattern(final String line, final String regex) {
        return "[\t ]+".equals(regex) ? DictionaryLineSplitter.split(line) : line.split(regex);
    }
}
