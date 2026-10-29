package github.kasuminova.novaeng.mixin.enumcache.ebwizardry;

import electroblob.wizardry.util.AllyDesignationSystem.FriendlyFire;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FriendlyFire.class, remap = false)
public abstract class MixinFriendlyFire {

    @Shadow
    @Final
    private static FriendlyFire[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FriendlyFire[] values() {
        return $VALUES;
    }
}
