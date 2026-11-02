package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockPressurePlate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockPressurePlate.Sensitivity.class, remap = false)
public abstract class MixinSensitivity {

    @Shadow
    @Final
    private static BlockPressurePlate.Sensitivity[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockPressurePlate.Sensitivity[] values() {
        return $VALUES;
    }
}
