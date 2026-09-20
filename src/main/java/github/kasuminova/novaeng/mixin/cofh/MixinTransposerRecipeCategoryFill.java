package github.kasuminova.novaeng.mixin.cofh;

import cofh.thermalexpansion.plugins.jei.machine.transposer.TransposerRecipeCategoryFill;
import mezz.jei.api.IModRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 跳过流体转置机在 JEI 中的装桶（填充）配方注册。
 *
 * <p>该类别会为每种可装桶流体生成一条配方，条目数量可观而实际查阅价值很低；
 * 抽取（排空）配方保持注册，因为它同样承载了流体与容器对应关系的查询用途。
 * 若需恢复，把 {@link #NOVA$DISABLE_FILL} 改为 false 即可。
 */
@Mixin(value = TransposerRecipeCategoryFill.class, remap = false)
public class MixinTransposerRecipeCategoryFill {

    @Unique
    private static final boolean NOVA$DISABLE_FILL = true;

    @Inject(method = "initialize", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nova$skipFillRecipes(final IModRegistry registry, final CallbackInfo ci) {
        if (NOVA$DISABLE_FILL) {
            ci.cancel();
        }
    }

}
