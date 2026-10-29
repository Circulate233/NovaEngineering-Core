package github.kasuminova.novaeng.mixin.enumcache.immersiveposts;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import twistedgate.immersiveposts.enums.EnumPostType;

@Mixin(value = EnumPostType.class, remap = false)
public abstract class MixinEnumPostType {

    @Shadow
    @Final
    private static EnumPostType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumPostType[] values() {
        return $VALUES;
    }
}
