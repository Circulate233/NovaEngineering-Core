package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.monster.AbstractIllager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AbstractIllager.IllagerArmPose.class, remap = false)
public abstract class MixinIllagerArmPose {

    @Shadow
    @Final
    private static AbstractIllager.IllagerArmPose[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AbstractIllager.IllagerArmPose[] values() {
        return $VALUES;
    }
}
