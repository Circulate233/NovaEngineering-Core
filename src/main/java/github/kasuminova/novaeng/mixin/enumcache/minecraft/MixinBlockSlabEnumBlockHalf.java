package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockSlab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockSlab.EnumBlockHalf.class, remap = false)
public abstract class MixinBlockSlabEnumBlockHalf {

    @Shadow
    @Final
    private static BlockSlab.EnumBlockHalf[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockSlab.EnumBlockHalf[] values() {
        return $VALUES;
    }
}
