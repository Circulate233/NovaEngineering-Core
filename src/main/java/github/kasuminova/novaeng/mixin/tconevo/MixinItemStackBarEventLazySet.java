package github.kasuminova.novaeng.mixin.tconevo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Marker: preApply uses RuntimeEventAllocationTransform for the constructor expression. */
@Pseudo
@Mixin(targets = "xyz.phanta.tconevo.client.event.ItemStackBarEvent", remap = false)
public abstract class MixinItemStackBarEventLazySet { }
