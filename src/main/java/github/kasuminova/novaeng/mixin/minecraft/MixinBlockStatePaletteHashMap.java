package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.world.chunk.BlockStatePaletteHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Grows a chunk section's block state palette by two bits at a time instead of one, so the section is rebuilt through
 * a fresh palette half as often. See {@link MixinBlockStatePaletteLinear} for why the step must grow the width and
 * what the change costs.
 */
@Mixin(BlockStatePaletteHashMap.class)
public class MixinBlockStatePaletteHashMap {

    @ModifyArg(method = "idFor", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/chunk/IBlockStatePaletteResizer;onResize(ILnet/minecraft/block/state/IBlockState;)I"),
        index = 0)
    private int nova$growByTwoBits(final int bits) {
        return Math.min(bits + 2, 9);
    }
}
