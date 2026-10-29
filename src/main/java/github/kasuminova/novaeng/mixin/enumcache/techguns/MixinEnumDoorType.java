package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.EnumDoorType;

@Mixin(value = EnumDoorType.class, remap = false)
public abstract class MixinEnumDoorType {

    @Shadow
    @Final
    private static EnumDoorType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumDoorType[] values() {
        return $VALUES;
    }
}
