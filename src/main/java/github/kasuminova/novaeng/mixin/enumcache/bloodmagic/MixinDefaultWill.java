package github.kasuminova.novaeng.mixin.enumcache.bloodmagic;

import WayofTime.bloodmagic.compat.jei.forge.TartaricForgeRecipeJEI.DefaultWill;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DefaultWill.class, remap = false)
public abstract class MixinDefaultWill {

    @Shadow
    @Final
    private static DefaultWill[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DefaultWill[] values() {
        return $VALUES;
    }
}
