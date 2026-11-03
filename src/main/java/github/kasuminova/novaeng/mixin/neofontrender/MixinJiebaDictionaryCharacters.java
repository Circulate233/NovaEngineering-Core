package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.common.performance.DictionaryCharacters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// DictSegment is package-private; the mixin does not depend on its concrete type.
@Mixin(targets = "com.huaban.analysis.jieba.DictSegment", remap = false)
public abstract class MixinJiebaDictionaryCharacters {

    @Redirect(method = "fillSegment([CIII)V",
        at = @At(value = "NEW", target = "(C)Ljava/lang/Character;"), require = 1)
    private Character nova$reuseCharacter(final char value) {
        return DictionaryCharacters.get(value);
    }
}
