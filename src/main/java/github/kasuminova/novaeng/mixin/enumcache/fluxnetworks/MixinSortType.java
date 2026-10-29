package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.client.gui.basic.GuiTabPages.SortType;

@Mixin(value = SortType.class, remap = false)
public abstract class MixinSortType {

    @Shadow
    @Final
    private static SortType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SortType[] values() {
        return $VALUES;
    }
}
