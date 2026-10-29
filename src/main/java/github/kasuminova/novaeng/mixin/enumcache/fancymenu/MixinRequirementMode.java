package github.kasuminova.novaeng.mixin.enumcache.fancymenu;

import de.keksuccino.fancymenu.menu.loadingrequirement.v2.internal.LoadingRequirementInstance.RequirementMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RequirementMode.class, remap = false)
public abstract class MixinRequirementMode {

    @Shadow
    @Final
    private static RequirementMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RequirementMode[] values() {
        return $VALUES;
    }
}
