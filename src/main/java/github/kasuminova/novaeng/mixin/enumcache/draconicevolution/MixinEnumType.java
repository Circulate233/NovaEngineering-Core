package github.kasuminova.novaeng.mixin.enumcache.draconicevolution;

import com.brandon3055.draconicevolution.blocks.DraconiumOre.EnumType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumType.class, remap = false)
public abstract class MixinEnumType {

    @Shadow
    @Final
    private static EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumType[] values() {
        return $VALUES;
    }
}
