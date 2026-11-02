package github.kasuminova.novaeng.mixin.enumcache.jaopca;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import thelm.jaopca.api.EnumEntryType;

@Mixin(value = EnumEntryType.class, remap = false)
public abstract class MixinEnumEntryType {

    @Shadow
    @Final
    private static EnumEntryType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumEntryType[] values() {
        return $VALUES;
    }
}
