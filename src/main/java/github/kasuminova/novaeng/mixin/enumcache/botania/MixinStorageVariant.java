package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.StorageVariant;

@Mixin(value = StorageVariant.class, remap = false)
public abstract class MixinStorageVariant {

    @Shadow
    @Final
    private static StorageVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static StorageVariant[] values() {
        return $VALUES;
    }
}
