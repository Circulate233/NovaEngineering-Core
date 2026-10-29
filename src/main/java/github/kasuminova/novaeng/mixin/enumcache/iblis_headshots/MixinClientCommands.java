package github.kasuminova.novaeng.mixin.enumcache.iblis_headshots;

import iblis_headshots.ServerNetworkHandler.ClientCommands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ClientCommands.class, remap = false)
public abstract class MixinClientCommands {

    @Shadow
    @Final
    private static ClientCommands[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ClientCommands[] values() {
        return $VALUES;
    }
}
