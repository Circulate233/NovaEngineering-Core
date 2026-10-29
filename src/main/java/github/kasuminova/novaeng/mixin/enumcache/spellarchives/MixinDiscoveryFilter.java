package github.kasuminova.novaeng.mixin.enumcache.spellarchives;

import com.spellarchives.gui.GuiSpellArchive.DiscoveryFilter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DiscoveryFilter.class, remap = false)
public abstract class MixinDiscoveryFilter {

    @Shadow
    @Final
    private static DiscoveryFilter[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DiscoveryFilter[] values() {
        return $VALUES;
    }
}
