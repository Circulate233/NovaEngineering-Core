package github.kasuminova.novaeng.mixin.enumcache.ancientspellcraft;

import com.windanesz.ancientspellcraft.util.LangUtils.ElderFuthark;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ElderFuthark.class, remap = false)
public abstract class MixinElderFuthark {

    @Shadow
    @Final
    private static ElderFuthark[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ElderFuthark[] values() {
        return $VALUES;
    }
}
