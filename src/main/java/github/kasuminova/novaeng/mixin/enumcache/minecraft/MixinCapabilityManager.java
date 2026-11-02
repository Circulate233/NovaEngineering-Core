package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.capabilities.CapabilityManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CapabilityManager.class, remap = false)
public abstract class MixinCapabilityManager {

    @Shadow
    @Final
    private static CapabilityManager[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CapabilityManager[] values() {
        return $VALUES;
    }
}
