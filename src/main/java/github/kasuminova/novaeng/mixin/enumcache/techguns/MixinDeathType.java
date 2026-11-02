package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.deatheffects.EntityDeathUtils.DeathType;

@Mixin(value = DeathType.class, remap = false)
public abstract class MixinDeathType {

    @Shadow
    @Final
    private static DeathType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DeathType[] values() {
        return $VALUES;
    }
}
