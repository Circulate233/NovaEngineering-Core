package github.kasuminova.novaeng.mixin.enderio;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Marker for the guarded whole-expression transform in the mixin plugin. */
@Pseudo
@Mixin(targets = "crazypants.enderio.base.render.model.RelayingBakedModel", remap = false)
public abstract class MixinRelayingModelProfilerLabels {
}
