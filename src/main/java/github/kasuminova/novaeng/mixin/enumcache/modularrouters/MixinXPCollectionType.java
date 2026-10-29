package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.integration.XPCollection.XPCollectionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = XPCollectionType.class, remap = false)
public abstract class MixinXPCollectionType {

    @Shadow
    @Final
    private static XPCollectionType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static XPCollectionType[] values() {
        return $VALUES;
    }
}
