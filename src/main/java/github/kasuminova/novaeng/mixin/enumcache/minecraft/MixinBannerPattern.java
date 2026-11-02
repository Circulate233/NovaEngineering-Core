package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.tileentity.BannerPattern;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BannerPattern.class, remap = false)
public abstract class MixinBannerPattern {

    @Shadow
    @Final
    private static BannerPattern[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BannerPattern[] values() {
        return $VALUES;
    }
}
