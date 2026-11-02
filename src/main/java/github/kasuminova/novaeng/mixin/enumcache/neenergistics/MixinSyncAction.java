package github.kasuminova.novaeng.mixin.enumcache.neenergistics;

import com.github.vfyjxf.nee.network.SyncAction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SyncAction.class, remap = false)
public abstract class MixinSyncAction {

    @Shadow
    @Final
    private static SyncAction[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SyncAction[] values() {
        return $VALUES;
    }
}
