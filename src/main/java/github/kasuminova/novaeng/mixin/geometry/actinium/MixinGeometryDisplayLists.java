package github.kasuminova.novaeng.mixin.geometry.actinium;

import com.dhj.actinium.render.FastLitItemDisplayListCache;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.client.geometry.CompactModel;
import github.kasuminova.novaeng.client.geometry.item.CompactItemModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.BakedItemModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = FastLitItemDisplayListCache.class, remap = false)
public abstract class MixinGeometryDisplayLists {
    @Shadow
    @Final
    private static String VANILLA_MODEL_WRAPPER_BAKED;

    /**
     * @author Nova Engineering
     * @reason Admit private compact item geometry without allocating a cancellable return callback.
     */
    @Overwrite
    public static boolean isStableModel(final IBakedModel model) {
        if (model instanceof CompactItemModel item)
            return item.novaeng$isItemFormat() && !item.novaeng$isGeometryExposed();
        if (model instanceof CompactModel compact && compact.novaeng$isGeometryExposed()) return false;
        final Class<?> type = model.getClass();
        if (type == SimpleBakedModel.class || type == BakedItemModel.class || type == BakedItemModel.BakedGuiItemModel.class)
            return true;
        return VANILLA_MODEL_WRAPPER_BAKED.equals(type.getName());
    }

    @Shadow
    private static int getCacheability(final List<BakedQuad> quads) {
        throw new AssertionError();
    }

    @Redirect(method = "getOrCompile", at = @At(value = "INVOKE", target = "Lcom/dhj/actinium/render/FastLitItemDisplayListCache;getCacheability(Ljava/util/List;)I"), require = 1)
    private static int novaeng$format(final List<BakedQuad> quads,
                                      @Local(argsOnly = true) final IBakedModel model) {
        return model instanceof CompactItemModel item && item.novaeng$isItemFormat() && !item.novaeng$isGeometryExposed() ? 0 : getCacheability(quads);
    }

    @Redirect(method = "getOrCompile", at = @At(value = "INVOKE", target = "Lcom/dhj/actinium/render/FastLitItemDisplayListCache;getQuadColors(Lnet/minecraft/client/renderer/color/ItemColors;Ljava/util/List;ILnet/minecraft/item/ItemStack;)[I"), require = 1)
    private static int[] novaeng$colors(final ItemColors itemColors, final List<BakedQuad> quads,
                                        final int color, final ItemStack stack,
                                        @Local(argsOnly = true) final IBakedModel model) {
        // Arbitrary tint callbacks may vary per invocation: keep the original per-quad sampling for them.
        if (model instanceof CompactItemModel item && item.novaeng$isItemFormat() && !item.novaeng$isGeometryExposed() && (color != -1 || stack == null || stack.isEmpty() ||
            !((AccessorGeometryItemColors) itemColors).novaeng$colors().containsKey(stack.getItem().delegate)))
            return new int[]{color};
        return FastLitItemDisplayListCache.getQuadColors(itemColors, quads, color, stack);
    }
}
