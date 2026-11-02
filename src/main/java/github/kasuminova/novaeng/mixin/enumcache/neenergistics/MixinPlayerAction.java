package github.kasuminova.novaeng.mixin.enumcache.neenergistics;

import com.github.vfyjxf.nee.network.PlayerAction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PlayerAction.class, remap = false)
public abstract class MixinPlayerAction {

    @Shadow
    @Final
    private static PlayerAction[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PlayerAction[] values() {
        return $VALUES;
    }
}
