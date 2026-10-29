package github.kasuminova.novaeng.mixin.enumcache.tinymobfarm;

import cn.davidma.tinymobfarm.core.EnumMobFarm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumMobFarm.class, remap = false)
public abstract class MixinEnumMobFarm {

    @Shadow
    @Final
    private static EnumMobFarm[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumMobFarm[] values() {
        return $VALUES;
    }
}
