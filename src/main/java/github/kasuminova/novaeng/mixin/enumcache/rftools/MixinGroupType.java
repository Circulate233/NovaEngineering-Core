package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.blocks.logic.sensor.GroupType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GroupType.class, remap = false)
public abstract class MixinGroupType {

    @Shadow
    @Final
    private static GroupType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GroupType[] values() {
        return $VALUES;
    }
}
