package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.api.utils.NBTType;

@Mixin(value = NBTType.class, remap = false)
public abstract class MixinNBTType {

    @Shadow
    @Final
    private static NBTType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NBTType[] values() {
        return $VALUES;
    }
}
