package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.api.gui.EnumNavigationTabs;

@Mixin(value = EnumNavigationTabs.class, remap = false)
public abstract class MixinEnumNavigationTabs {

    @Shadow
    @Final
    private static EnumNavigationTabs[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumNavigationTabs[] values() {
        return $VALUES;
    }
}
