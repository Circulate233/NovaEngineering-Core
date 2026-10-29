package github.kasuminova.novaeng.mixin.enumcache.ingameime;

import com.dhj.ingameime.theme.ThemeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ThemeType.class, remap = false)
public abstract class MixinThemeType {

    @Shadow
    @Final
    private static ThemeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ThemeType[] values() {
        return $VALUES;
    }
}
