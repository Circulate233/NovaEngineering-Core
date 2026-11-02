package github.kasuminova.novaeng.mixin.enumcache.fancymenu;

import de.keksuccino.fancymenu.menu.loadingrequirement.v2.internal.LoadingRequirementGroup.GroupMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GroupMode.class, remap = false)
public abstract class MixinGroupMode {

    @Shadow
    @Final
    private static GroupMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GroupMode[] values() {
        return $VALUES;
    }
}
