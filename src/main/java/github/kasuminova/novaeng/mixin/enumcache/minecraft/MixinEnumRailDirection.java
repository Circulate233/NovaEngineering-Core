package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockRailBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockRailBase.EnumRailDirection.class, remap = false)
public abstract class MixinEnumRailDirection {

    @Shadow
    @Final
    private static BlockRailBase.EnumRailDirection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockRailBase.EnumRailDirection[] values() {
        return $VALUES;
    }
}
