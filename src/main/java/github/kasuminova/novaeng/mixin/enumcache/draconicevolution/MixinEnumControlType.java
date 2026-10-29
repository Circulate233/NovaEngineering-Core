package github.kasuminova.novaeng.mixin.enumcache.draconicevolution;

import com.brandon3055.draconicevolution.api.itemconfig.IItemConfigField.EnumControlType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumControlType.class, remap = false)
public abstract class MixinEnumControlType {

    @Shadow
    @Final
    private static EnumControlType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumControlType[] values() {
        return $VALUES;
    }
}
