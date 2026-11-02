package github.kasuminova.novaeng.mixin.enumcache.artisanworktables;

import com.codetaylor.mc.artisanworktables.api.internal.reference.EnumTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumTier.class, remap = false)
public abstract class MixinEnumTier {

    @Shadow
    @Final
    private static EnumTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumTier[] values() {
        return $VALUES;
    }
}
