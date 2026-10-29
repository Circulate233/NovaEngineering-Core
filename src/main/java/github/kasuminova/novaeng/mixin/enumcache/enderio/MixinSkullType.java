package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.block.skull.SkullType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SkullType.class, remap = false)
public abstract class MixinSkullType {

    @Shadow
    @Final
    private static SkullType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SkullType[] values() {
        return $VALUES;
    }
}
