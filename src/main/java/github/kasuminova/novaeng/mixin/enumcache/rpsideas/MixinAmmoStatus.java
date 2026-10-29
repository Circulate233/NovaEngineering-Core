package github.kasuminova.novaeng.mixin.enumcache.rpsideas;

import com.kamefrede.rpsideas.entity.EntityGaussPulse.AmmoStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AmmoStatus.class, remap = false)
public abstract class MixinAmmoStatus {

    @Shadow
    @Final
    private static AmmoStatus[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AmmoStatus[] values() {
        return $VALUES;
    }
}
