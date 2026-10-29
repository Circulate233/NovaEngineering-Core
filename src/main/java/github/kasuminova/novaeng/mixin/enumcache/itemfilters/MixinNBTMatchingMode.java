package github.kasuminova.novaeng.mixin.enumcache.itemfilters;

import com.latmod.mods.itemfilters.filters.NBTMatchingMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NBTMatchingMode.class, remap = false)
public abstract class MixinNBTMatchingMode {

    @Shadow
    @Final
    private static NBTMatchingMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NBTMatchingMode[] values() {
        return $VALUES;
    }
}
