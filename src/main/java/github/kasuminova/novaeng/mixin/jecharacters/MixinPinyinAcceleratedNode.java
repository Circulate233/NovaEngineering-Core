package github.kasuminova.novaeng.mixin.jecharacters;

import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import me.towdium.pinin.searchers.TreeSearcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = TreeSearcher.NAcc.class, remap = false)
public abstract class MixinPinyinAcceleratedNode {
    @Unique
    private CharOpenHashSet nova$indexedCharacters;

    @Shadow
    private void index(final TreeSearcher<?> p, final char c) {
        throw new AssertionError();
    }

    @Redirect(method = "put(Lme/towdium/pinin/searchers/TreeSearcher;II)Lme/towdium/pinin/searchers/TreeSearcher$NAcc;",
        at = @At(value = "INVOKE",
            target = "Lme/towdium/pinin/searchers/TreeSearcher$NAcc;index(Lme/towdium/pinin/searchers/TreeSearcher;C)V"),
        require = 1)
    private void nova$indexOnce(final TreeSearcher.NAcc<?> node, final TreeSearcher<?> p, final char c) {
        if (nova$indexedCharacters == null) {
            nova$indexedCharacters = new CharOpenHashSet(32);
        }
        if (!nova$indexedCharacters.contains(c)) {
            index(p, c);
            nova$indexedCharacters.add(c);
        }
    }

    @Redirect(method = "reload", at = @At(value = "INVOKE", target = "Ljava/util/Map;clear()V"), require = 1)
    private void nova$resetIndexedCharacters(final Map<?, ?> index) {
        index.clear();
        if (nova$indexedCharacters != null) {
            nova$indexedCharacters.clear();
        }
    }
}
