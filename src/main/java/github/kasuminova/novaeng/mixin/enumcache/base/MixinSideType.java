package github.kasuminova.novaeng.mixin.enumcache.base;

import com.teamacronymcoders.base.blocks.properties.SideType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SideType.class, remap = false)
public abstract class MixinSideType {

    @Shadow
    @Final
    private static SideType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SideType[] values() {
        return $VALUES;
    }
}
