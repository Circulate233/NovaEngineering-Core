package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.EnumConnectionType;

@Mixin(value = EnumConnectionType.class, remap = false)
public abstract class MixinEnumConnectionType {

    @Shadow
    @Final
    private static EnumConnectionType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumConnectionType[] values() {
        return $VALUES;
    }
}
