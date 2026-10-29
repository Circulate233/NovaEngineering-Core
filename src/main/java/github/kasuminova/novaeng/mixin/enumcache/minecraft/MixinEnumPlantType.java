package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.EnumPlantType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumPlantType.class, remap = false)
public abstract class MixinEnumPlantType {

    @Shadow
    @Final
    private static EnumPlantType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumPlantType[] values() {
        return $VALUES;
    }
}
