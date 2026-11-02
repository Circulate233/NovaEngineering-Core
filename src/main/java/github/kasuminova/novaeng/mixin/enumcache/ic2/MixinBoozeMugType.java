package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.item.ItemBooze.BoozeMugType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BoozeMugType.class, remap = false)
public abstract class MixinBoozeMugType {

    @Shadow
    @Final
    private static BoozeMugType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BoozeMugType[] values() {
        return $VALUES;
    }
}
