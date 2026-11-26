package github.kasuminova.novaeng.mixin.zenscript;

import org.spongepowered.asm.mixin.Mixin;
import stanhebben.zenscript.ZenTokener;

/**
 * Marker for the guarded whole-expression patch in the mixin config plugin.
 */
@Mixin(value = ZenTokener.class, remap = false)
public abstract class MixinZenTokenerDfa {
}
