package github.kasuminova.novaeng.mixin.reid;

import github.kasuminova.novaeng.common.performance.SectionStorageAccess;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BitArray;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.BlockStatePaletteHashMap;
import net.minecraft.world.chunk.BlockStatePaletteLinear;
import net.minecraft.world.chunk.IBlockStatePalette;
import net.minecraft.world.chunk.IBlockStatePaletteResizer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockStateContainer.class, priority = 500)
public abstract class MixinSectionStorage implements SectionStorageAccess {
    @Shadow
    @Final
    protected static IBlockState AIR_BLOCK_STATE;
    @Shadow
    @Final
    private static IBlockStatePalette REGISTRY_BASED_PALETTE;
    @Shadow
    protected BitArray storage;
    @Shadow
    protected IBlockStatePalette palette;
    @Shadow
    private int bits;

    @Override
    public void nova$initializeSection(final int requestedBits) {
        final IBlockStatePaletteResizer self = (IBlockStatePaletteResizer) this;
        bits = Math.max(4, requestedBits);
        if (bits == 4) {
            palette = new BlockStatePaletteLinear(bits, self);
        } else if (bits <= 8) {
            palette = new BlockStatePaletteHashMap(bits, self);
        } else {
            palette = REGISTRY_BASED_PALETTE;
            bits = MathHelper.log2DeBruijn(Block.BLOCK_STATE_IDS.size());
        }
        palette.idFor(AIR_BLOCK_STATE);
        storage = new BitArray(bits, 4096);
    }

    @Override
    public void nova$writeSectionState(final int index, final IBlockState state) {
        storage.setAt(index, palette.idFor(state));
    }

    @Override
    public IBlockState nova$readSectionState(final int index) {
        final IBlockState state = palette.getBlockState(storage.getAt(index));
        return state == null ? AIR_BLOCK_STATE : state;
    }
}
