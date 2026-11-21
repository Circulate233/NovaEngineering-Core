package github.kasuminova.novaeng.mixin.reid;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.performance.PackedSectionBits;
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
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;

@Mixin(value = BlockStateContainer.class, priority = 500)
public abstract class MixinSectionStorage implements SectionStorageAccess {
    @Unique
    private static final ThreadLocal<int[]> NOVA_TRANSLATED_IDS =
        ThreadLocal.withInitial(() -> new int[1 << 12]);

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

    @Unique
    private IBlockState nova$lastSetState;
    @Unique
    private IBlockStatePalette nova$lastSetPalette;
    @Unique
    private int nova$lastSetId;

    @Override
    public void nova$invalidatePaletteSetCache() {
        this.nova$lastSetState = null;
        this.nova$lastSetPalette = null;
    }

    /**
     * @author NovaEngineering
     * @reason Preserve local palette IDs during growth and translate each old ID once on fallback paths.
     */
    @Overwrite
    public int onResize(final int requestedBits, final IBlockState state) {
        final BitArray oldStorage = storage;
        final IBlockStatePalette oldPalette = palette;
        final int oldBits = bits;

        final boolean localGrowth = requestedBits == oldBits + 1 && oldBits >= 4 && oldBits < 8;
        final int growthBits = localGrowth && NovaEngCoreConfig.SERVER.reserveChunkPaletteCapacity
            ? Math.min(8, (requestedBits + 1) & ~1) : requestedBits;
        nova$initializeSection(growthBits);

        if (localGrowth && (oldPalette.getClass() == BlockStatePaletteLinear.class
            || oldPalette.getClass() == BlockStatePaletteHashMap.class)) {
            boolean sameIds = true;
            for (int id = 0, capacity = 1 << oldBits; id < capacity; id++) {
                final IBlockState oldState = oldPalette.getBlockState(id);
                if (oldState == null || palette.idFor(oldState) != id) {
                    sameIds = false;
                    break;
                }
            }
            if (sameIds) {
                PackedSectionBits.widen(oldStorage.getBackingLongArray(), oldBits,
                    storage.getBackingLongArray(), bits, oldStorage.size());
                return palette.idFor(state);
            }
        }

        // Forced network widths can be much larger than a normal local
        // palette. Do not allocate an enormous id translation table there.
        if (oldBits > 12) {
            for (int i = 0; i < oldStorage.size(); ++i) {
                final IBlockState oldState = oldPalette.getBlockState(oldStorage.getAt(i));
                if (oldState != null) {
                    storage.setAt(i, palette.idFor(oldState));
                }
            }
            return palette.idFor(state);
        }

        // Zero means "unseen"; a real palette id is stored as id + 1.
        // A null old entry is remembered as -1 so malformed input retains
        // vanilla's skip behavior without repeating the lookup 4096 times.
        final int translationSize = 1 << oldBits;
        final int[] translatedIds = NOVA_TRANSLATED_IDS.get();
        Arrays.fill(translatedIds, 0, translationSize, 0);
        for (int i = 0; i < oldStorage.size(); ++i) {
            final int oldId = oldStorage.getAt(i);
            int translated = translatedIds[oldId];
            if (translated == 0) {
                final IBlockState oldState = oldPalette.getBlockState(oldId);
                translated = oldState == null ? -1 : palette.idFor(oldState) + 1;
                translatedIds[oldId] = translated;
            }
            if (translated > 0) {
                storage.setAt(i, translated - 1);
            }
        }
        return palette.idFor(state);
    }

    @Redirect(
        method = "set(ILnet/minecraft/block/state/IBlockState;)V",
        at =
        @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/world/chunk/IBlockStatePalette;idFor(Lnet/minecraft/block/state/IBlockState;)I"),
        require = 1)
    private int nova$cachedSetId(final IBlockStatePalette currentPalette, final IBlockState state) {
        if (currentPalette == this.nova$lastSetPalette && state == this.nova$lastSetState) {
            return this.nova$lastSetId;
        }
        final int id = currentPalette.idFor(state);
        this.nova$lastSetPalette = currentPalette;
        this.nova$lastSetState = state;
        this.nova$lastSetId = id;
        return id;
    }

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
    public int nova$sectionId(final IBlockState state) {
        return palette.idFor(state);
    }

    @Override
    public void nova$writeSectionId(final int index, final int paletteId) {
        storage.setAt(index, paletteId);
    }

    @Override
    public void nova$importLocalIds(final short[] ids, final int offset, final IBlockState[] states,
                                    final int stateCount, final int[] translatedIds) {
        final int targetBits = Math.max(4, 32 - Integer.numberOfLeadingZeros(stateCount - 1));
        // This entry point receives a new, exclusively owned section; reuse its initial 4-bit storage.
        if (targetBits != bits) nova$initializeSection(targetBits);
        for (int i = 0; i < stateCount; i++) translatedIds[i] = palette.idFor(states[i]);
        PackedSectionBits.pack(ids, offset, translatedIds, storage.getBackingLongArray(), bits, 4096);
    }

    @Override
    public void nova$fillSection(final IBlockState state) {
        if (bits != 4) nova$initializeSection(4);
        final int id = palette.idFor(state);
        Arrays.fill(storage.getBackingLongArray(), (id & 15L) * 0x1111111111111111L);
    }

    @Override
    public IBlockState nova$readSectionState(final int index) {
        final IBlockState state = palette.getBlockState(storage.getAt(index));
        return state == null ? AIR_BLOCK_STATE : state;
    }

    @Override
    public int nova$sectionBits() {
        return bits;
    }

    @Override
    public int nova$readSectionId(final int index) {
        return storage.getAt(index);
    }

    @Override
    public IBlockState nova$stateForPaletteId(final int id) {
        final IBlockState state = palette.getBlockState(id);
        return state == null ? AIR_BLOCK_STATE : state;
    }
}
