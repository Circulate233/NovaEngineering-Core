package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.EnumOreClusterType;

@Mixin(value = EnumOreClusterType.class, remap = false)
public abstract class MixinEnumOreClusterType {

    @Shadow
    @Final
    private static EnumOreClusterType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumOreClusterType[] values() {
        return $VALUES;
    }
}
