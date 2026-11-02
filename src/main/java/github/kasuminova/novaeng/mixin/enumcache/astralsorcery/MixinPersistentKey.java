package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.client.data.PersistentDataManager.PersistentKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PersistentKey.class, remap = false)
public abstract class MixinPersistentKey {

    @Shadow
    @Final
    private static PersistentKey[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PersistentKey[] values() {
        return $VALUES;
    }
}
