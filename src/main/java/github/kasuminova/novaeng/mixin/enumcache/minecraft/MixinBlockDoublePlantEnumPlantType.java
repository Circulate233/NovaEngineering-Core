package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockDoublePlant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockDoublePlant.EnumPlantType.class, remap = false)
public abstract class MixinBlockDoublePlantEnumPlantType {

    @Shadow
    @Final
    private static BlockDoublePlant.EnumPlantType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockDoublePlant.EnumPlantType[] values() {
        return $VALUES;
    }
}
