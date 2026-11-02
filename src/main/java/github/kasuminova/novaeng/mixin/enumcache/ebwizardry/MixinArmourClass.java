package github.kasuminova.novaeng.mixin.enumcache.ebwizardry;

import electroblob.wizardry.item.ItemWizardArmour.ArmourClass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ArmourClass.class, remap = false)
public abstract class MixinArmourClass {

    @Shadow
    @Final
    private static ArmourClass[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ArmourClass[] values() {
        return $VALUES;
    }
}
