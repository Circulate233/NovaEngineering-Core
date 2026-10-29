package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockPurpurSlab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockPurpurSlab.Variant.class, remap = false)
public abstract class MixinVariant {

    @Shadow
    @Final
    private static BlockPurpurSlab.Variant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockPurpurSlab.Variant[] values() {
        return $VALUES;
    }
}
