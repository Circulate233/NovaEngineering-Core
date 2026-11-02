package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.item.module.Module.RelativeDirection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RelativeDirection.class, remap = false)
public abstract class MixinRelativeDirection {

    @Shadow
    @Final
    private static RelativeDirection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RelativeDirection[] values() {
        return $VALUES;
    }
}
