package github.kasuminova.novaeng.mixin.enumcache.futuremc;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import thedarkcolour.futuremc.entity.fish.tropical.EntityTropicalFish.Type;

@Mixin(value = Type.class, remap = false)
public abstract class MixinType {

    @Shadow
    @Final
    private static Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Type[] values() {
        return $VALUES;
    }
}
