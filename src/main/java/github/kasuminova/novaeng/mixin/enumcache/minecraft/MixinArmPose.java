package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.model.ModelBiped;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModelBiped.ArmPose.class, remap = false)
public abstract class MixinArmPose {

    @Shadow
    @Final
    private static ModelBiped.ArmPose[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModelBiped.ArmPose[] values() {
        return $VALUES;
    }
}
