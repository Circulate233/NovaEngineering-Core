package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockStoneBrick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockStoneBrick.EnumType.class, remap = false)
public abstract class MixinBlockStoneBrickEnumType {

    @Shadow
    @Final
    private static BlockStoneBrick.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockStoneBrick.EnumType[] values() {
        return $VALUES;
    }
}
