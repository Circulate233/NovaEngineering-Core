package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.mmce.common.util.OredictCache;
import github.kasuminova.novaeng.common.performance.OreIdsCache;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = OredictCache.class, remap = false)
public abstract class MixinOredictCache {
    /**
     * @author NovaEngineering
     * @reason Use matching primitive read/write keys and invalidate after ore changes.
     */
    @Overwrite
    public static int[] getOreIDsFast(final ItemStack stack) {
        return OreIdsCache.get(stack);
    }
}
