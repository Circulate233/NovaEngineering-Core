package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.conduit.ConnectionMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConnectionMode.class, remap = false)
public abstract class MixinConnectionMode {

    @Shadow
    @Final
    private static ConnectionMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConnectionMode[] values() {
        return $VALUES;
    }
}
