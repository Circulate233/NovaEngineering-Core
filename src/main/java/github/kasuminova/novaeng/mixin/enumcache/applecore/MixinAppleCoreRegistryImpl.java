package github.kasuminova.novaeng.mixin.enumcache.applecore;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import squeek.applecore.api_impl.AppleCoreRegistryImpl;

@Mixin(value = AppleCoreRegistryImpl.class, remap = false)
public abstract class MixinAppleCoreRegistryImpl {

    @Shadow
    @Final
    private static AppleCoreRegistryImpl[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AppleCoreRegistryImpl[] values() {
        return $VALUES;
    }
}
