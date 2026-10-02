package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.world.chunk.BlockStatePaletteLinear;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Grows a chunk section's block state palette by two bits at a time instead of one.
 *
 * <p>Every overflow makes {@code BlockStateContainer#onResize} build a fresh palette and re-insert all 4096 entries of
 * the section through it, so a section that ends up with a few hundred different states - which is what a built up
 * area looks like - pays that rebuild three or four times. Reading a section from NBT is exactly that case, because
 * JustEnoughIDs re-inserts the states one by one, and a world load reads hundreds of sections.</p>
 *
 * <p>The step has to grow the width: {@code setBits} returns immediately when it is handed the width it already has,
 * and {@code onResize} then re-inserts the entries and asks the same palette for the id of the new state again, which
 * would recurse until the stack overflows. Two bits per step keeps the ladder strictly increasing and only skips the
 * 32 entry width, so the sole lasting difference is a section holding 17 to 32 different states keeping a 6 bit width
 * instead of 5, i.e. 512 bytes for that section. Every other section ends at the width it had before.</p>
 */
@Mixin(BlockStatePaletteLinear.class)
public class MixinBlockStatePaletteLinear {

    @ModifyArg(method = "idFor", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/chunk/IBlockStatePaletteResizer;onResize(ILnet/minecraft/block/state/IBlockState;)I"),
        index = 0)
    private int nova$growByTwoBits(final int bits) {
        return Math.min(bits + 2, 9);
    }
}
