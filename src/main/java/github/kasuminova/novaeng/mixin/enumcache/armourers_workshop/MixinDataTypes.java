package github.kasuminova.novaeng.mixin.enumcache.armourers_workshop;

import moe.plushie.armourers_workshop.common.skin.data.SkinProperties.DataTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DataTypes.class, remap = false)
public abstract class MixinDataTypes {

    @Shadow
    @Final
    private static DataTypes[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DataTypes[] values() {
        return $VALUES;
    }
}
