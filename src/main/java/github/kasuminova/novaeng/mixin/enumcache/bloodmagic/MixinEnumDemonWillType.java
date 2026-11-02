package github.kasuminova.novaeng.mixin.enumcache.bloodmagic;

import WayofTime.bloodmagic.soul.EnumDemonWillType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumDemonWillType.class, remap = false)
public abstract class MixinEnumDemonWillType {

    @Shadow
    @Final
    private static EnumDemonWillType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumDemonWillType[] values() {
        return $VALUES;
    }
}
