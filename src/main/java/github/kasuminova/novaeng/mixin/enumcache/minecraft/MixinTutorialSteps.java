package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.tutorial.TutorialSteps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TutorialSteps.class, remap = false)
public abstract class MixinTutorialSteps {

    @Shadow
    @Final
    private static TutorialSteps[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TutorialSteps[] values() {
        return $VALUES;
    }
}
