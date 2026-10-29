package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.EnumMilitaryCrateType;

@Mixin(value = EnumMilitaryCrateType.class, remap = false)
public abstract class MixinEnumMilitaryCrateType {

    @Shadow
    @Final
    private static EnumMilitaryCrateType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumMilitaryCrateType[] values() {
        return $VALUES;
    }
}
