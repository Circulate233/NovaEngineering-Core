package github.kasuminova.novaeng.mixin.enumcache.bloodmagic;

import WayofTime.bloodmagic.item.types.AlchemicVialType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AlchemicVialType.class, remap = false)
public abstract class MixinAlchemicVialType {

    @Shadow
    @Final
    private static AlchemicVialType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AlchemicVialType[] values() {
        return $VALUES;
    }
}
