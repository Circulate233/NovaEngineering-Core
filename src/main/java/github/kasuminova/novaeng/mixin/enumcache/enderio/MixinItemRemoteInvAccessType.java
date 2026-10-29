package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.invpanel.remote.ItemRemoteInvAccessType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemRemoteInvAccessType.class, remap = false)
public abstract class MixinItemRemoteInvAccessType {

    @Shadow
    @Final
    private static ItemRemoteInvAccessType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ItemRemoteInvAccessType[] values() {
        return $VALUES;
    }
}
