package github.kasuminova.novaeng.mixin.draconicevolution;

import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import org.spongepowered.asm.mixin.Mixin;

/** Marker for FusionInjectorScanTransform's guarded expression replacement. */
@Mixin(value = TileFusionCraftingCore.class, remap = false)
public abstract class MixinFusionInjectorScan {
}
