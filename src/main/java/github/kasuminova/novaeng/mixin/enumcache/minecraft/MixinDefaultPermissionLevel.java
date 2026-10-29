package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.server.permission.DefaultPermissionLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DefaultPermissionLevel.class, remap = false)
public abstract class MixinDefaultPermissionLevel {

    @Shadow
    @Final
    private static DefaultPermissionLevel[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DefaultPermissionLevel[] values() {
        return $VALUES;
    }
}
