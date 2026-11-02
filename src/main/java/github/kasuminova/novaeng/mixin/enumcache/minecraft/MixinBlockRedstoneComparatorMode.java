package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.BlockRedstoneComparator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockRedstoneComparator.Mode.class, remap = false)
public abstract class MixinBlockRedstoneComparatorMode {

    @Shadow
    @Final
    private static BlockRedstoneComparator.Mode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockRedstoneComparator.Mode[] values() {
        return $VALUES;
    }
}
