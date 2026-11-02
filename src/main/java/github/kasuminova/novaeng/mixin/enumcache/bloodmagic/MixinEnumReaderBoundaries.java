package github.kasuminova.novaeng.mixin.enumcache.bloodmagic;

import WayofTime.bloodmagic.ritual.EnumReaderBoundaries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumReaderBoundaries.class, remap = false)
public abstract class MixinEnumReaderBoundaries {

    @Shadow
    @Final
    private static EnumReaderBoundaries[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumReaderBoundaries[] values() {
        return $VALUES;
    }
}
