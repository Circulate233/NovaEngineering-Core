package github.kasuminova.novaeng.mixin.enderutilities;

import fi.dy.masa.enderutilities.compat.jei.EnderUtilitiesJeiPlugin;
import mezz.jei.api.IModRegistry;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 修正 Ender Utilities 的 JEI 插件在被禁用的方块上注册配方催化剂的问题。
 *
 * <p>该插件的 {@code register} 直接引用 Ender Furnace 作为熔炼配方的催化剂，
 * 却没有判断该方块是否被配置禁用（{@code disableBlockMachine_0}）。
 * 方块未注册时 {@code ItemStack} 退化为空气，JEI 的校验随即抛出
 * {@code Invalid ingredient found}，导致整个插件被跳过，
 * 连带丢失 Creation Station 的配方转移与点击区域集成。
 *
 * <p>此处只过滤掉空气催化剂，其余注册流程保持原样。
 */
@Mixin(value = EnderUtilitiesJeiPlugin.class, remap = false)
public class MixinEnderUtilitiesJeiPlugin {

    @Redirect(
        method = "register",
        at = @At(
            value = "INVOKE",
            target = "Lmezz/jei/api/IModRegistry;addRecipeCatalyst(Ljava/lang/Object;[Ljava/lang/String;)V"
        ),
        remap = false
    )
    private void nova$skipEmptyCatalyst(IModRegistry registry, Object ingredient, String[] recipeTypes) {
        if (ingredient instanceof ItemStack stack && stack.isEmpty()) {
            return;
        }
        registry.addRecipeCatalyst(ingredient, recipeTypes);
    }

}
