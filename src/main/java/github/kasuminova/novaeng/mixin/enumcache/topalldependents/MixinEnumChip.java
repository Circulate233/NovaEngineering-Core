package github.kasuminova.novaeng.mixin.enumcache.topalldependents;

import com.wdcftgg.topalldependents.mods.topaddons.reference.EnumChip;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumChip.class, remap = false)
public abstract class MixinEnumChip {

    @Shadow
    @Final
    private static EnumChip[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumChip[] values() {
        return $VALUES;
    }
}
