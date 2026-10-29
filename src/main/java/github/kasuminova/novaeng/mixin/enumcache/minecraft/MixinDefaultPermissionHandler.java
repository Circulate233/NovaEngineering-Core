package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.server.permission.DefaultPermissionHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DefaultPermissionHandler.class, remap = false)
public abstract class MixinDefaultPermissionHandler {

    @Shadow
    @Final
    private static DefaultPermissionHandler[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DefaultPermissionHandler[] values() {
        return $VALUES;
    }
}
