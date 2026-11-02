package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockStoneSlab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockStoneSlab.EnumType.class, remap = false)
public abstract class MixinBlockStoneSlabEnumType {

    @Shadow
    @Final
    private static BlockStoneSlab.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockStoneSlab.EnumType[] values() {
        return $VALUES;
    }
}
