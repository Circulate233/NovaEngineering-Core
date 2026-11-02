package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.network.OpenGuiMessage.What;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = What.class, remap = false)
public abstract class MixinWhat {

    @Shadow
    @Final
    private static What[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static What[] values() {
        return $VALUES;
    }
}
