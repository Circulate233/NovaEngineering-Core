package github.kasuminova.novaeng.mixin.enumcache.applecore;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import squeek.applecore.api_impl.AppleCoreAccessorMutatorImpl;

@Mixin(value = AppleCoreAccessorMutatorImpl.class, remap = false)
public abstract class MixinAppleCoreAccessorMutatorImpl {

    @Shadow
    @Final
    private static AppleCoreAccessorMutatorImpl[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AppleCoreAccessorMutatorImpl[] values() {
        return $VALUES;
    }
}
