package github.kasuminova.novaeng.mixin.enumcache.environmentaltech;

import com.valkyrieofnight.et.base.block.EnumDecorativeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumDecorativeType.class, remap = false)
public abstract class MixinEnumDecorativeType {

    @Shadow
    @Final
    private static EnumDecorativeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumDecorativeType[] values() {
        return $VALUES;
    }
}
