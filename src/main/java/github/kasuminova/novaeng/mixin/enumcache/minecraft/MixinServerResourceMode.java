package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ServerData.ServerResourceMode.class, remap = false)
public abstract class MixinServerResourceMode {

    @Shadow
    @Final
    private static ServerData.ServerResourceMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ServerData.ServerResourceMode[] values() {
        return $VALUES;
    }
}
