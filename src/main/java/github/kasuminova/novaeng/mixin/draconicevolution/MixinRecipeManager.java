package github.kasuminova.novaeng.mixin.draconicevolution;

import com.brandon3055.draconicevolution.lib.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 修复 Draconic Evolution 融合配方对物品名的误判。
 *
 * <p>原实现用 {@code contains("ore:")} 判断条目是否为矿物词典引用，这是子串匹配而非前缀匹配。
 * 任何命名空间中含有 {@code ore:} 子串的物品名都会被误判，例如 {@code novaeng_core:xxx}
 * 中的 {@code ...c + ore:}，导致整条配方被拒绝加载。这里改为前缀匹配。
 */
@Mixin(value = RecipeManager.class, remap = false)
public class MixinRecipeManager {

    @Redirect(
        method = "readStackEntry",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z",
            ordinal = 1
        ),
        remap = false
    )
    private static boolean nova$orePrefixOnly(final String entry, final CharSequence s) {
        return entry.startsWith("ore:");
    }

}
