package github.kasuminova.novaeng.mixin.mekanism;

import github.kasuminova.novaeng.common.util.NovaResettableTarget;
import mekanism.common.base.target.Target;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

/**
 * Gives a network target a way to be emptied and refilled again, so the pooling in the network mixins can hand the
 * same instance out on later ticks.
 *
 * <p>A target is two enum maps plus a slot for whatever extra data the subclass keeps, all of which the constructor
 * allocates and the tick then fills in. None of it outlives the tick it was built for, and the extra slot is exactly
 * what {@code GasHandlerTarget}'s constructor argument sets.</p>
 */
@Mixin(value = Target.class, remap = false)
public abstract class MixinTargetReset implements NovaResettableTarget {

    @Shadow
    @Final
    protected Map<EnumFacing, Object> handlers;

    @Shadow
    @Final
    protected Map<EnumFacing, Object> needed;

    @Shadow
    protected Object extra;

    @Override
    public void nova$reset() {
        this.handlers.clear();
        this.needed.clear();
        this.extra = null;
    }

    @Override
    public void nova$setExtra(final Object extra) {
        this.extra = extra;
    }
}
