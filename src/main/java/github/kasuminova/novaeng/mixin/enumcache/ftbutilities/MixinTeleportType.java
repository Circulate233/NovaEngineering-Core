package github.kasuminova.novaeng.mixin.enumcache.ftbutilities;

import com.feed_the_beast.ftbutilities.data.TeleportType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TeleportType.class, remap = false)
public abstract class MixinTeleportType {

    @Shadow
    @Final
    private static TeleportType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TeleportType[] values() {
        return $VALUES;
    }
}
