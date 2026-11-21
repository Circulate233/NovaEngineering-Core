package github.kasuminova.novaeng.mixin.geometry.actinium;

import com.dhj.actinium.render.FastLitItemDisplayListCache;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.client.geometry.CompactModel;
import github.kasuminova.novaeng.client.geometry.item.CompactItemModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = FastLitItemDisplayListCache.class, remap = false)
public abstract class MixinGeometryDisplayLists {
    @Inject(method = "isStableModel", at = @At("HEAD"), cancellable = true)
    private static void novaeng$stable(IBakedModel model, CallbackInfoReturnable<Boolean> cir) {
        if (model instanceof CompactItemModel item)
            cir.setReturnValue(item.novaeng$isItemFormat() && !item.novaeng$isGeometryExposed());
        else if (model instanceof CompactModel compact && compact.novaeng$isGeometryExposed())
            cir.setReturnValue(false);
    }

    @WrapOperation(method = "getOrCompile", at = @At(value = "INVOKE", target = "Lcom/dhj/actinium/render/FastLitItemDisplayListCache;getCacheability(Ljava/util/List;)I"))
    private static int novaeng$format(List<BakedQuad> quads, Operation<Integer> original, @Local(argsOnly = true) IBakedModel model) {
        return model instanceof CompactItemModel item && item.novaeng$isItemFormat() && !item.novaeng$isGeometryExposed() ? 0 : original.call(quads);
    }

    @WrapOperation(method = "getOrCompile", at = @At(value = "INVOKE", target = "Lcom/dhj/actinium/render/FastLitItemDisplayListCache;getQuadColors(Lnet/minecraft/client/renderer/RenderItem;Ljava/util/List;ILnet/minecraft/item/ItemStack;)[I"))
    private static int[] novaeng$colors(RenderItem renderItem, List<BakedQuad> quads, int color, ItemStack stack, Operation<int[]> original, @Local(argsOnly = true) IBakedModel model) {
        // Arbitrary tint callbacks may vary per invocation: keep the original per-quad sampling for them.
        if (model instanceof CompactItemModel item && item.novaeng$isItemFormat() && !item.novaeng$isGeometryExposed() && (color != -1 || stack == null || stack.isEmpty() ||
            !((AccessorGeometryItemColors) Minecraft.getMinecraft().getItemColors()).novaeng$colors().containsKey(stack.getItem().delegate)))
            return new int[]{color};
        return original.call(renderItem, quads, color, stack);
    }
}
