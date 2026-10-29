package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockLog;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockLog.EnumAxis.class, remap = false)
public abstract class MixinEnumAxis {

    @Shadow
    @Final
    private static BlockLog.EnumAxis[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockLog.EnumAxis[] values() {
        return $VALUES;
    }
}
