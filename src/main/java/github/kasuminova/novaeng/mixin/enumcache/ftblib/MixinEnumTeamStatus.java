package github.kasuminova.novaeng.mixin.enumcache.ftblib;

import com.feed_the_beast.ftblib.lib.EnumTeamStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumTeamStatus.class, remap = false)
public abstract class MixinEnumTeamStatus {

    @Shadow
    @Final
    private static EnumTeamStatus[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumTeamStatus[] values() {
        return $VALUES;
    }
}
