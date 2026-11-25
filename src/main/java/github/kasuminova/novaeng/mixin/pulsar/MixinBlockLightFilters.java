package github.kasuminova.novaeng.mixin.pulsar;

import com.sumirelabs.pulsar.api.BlockLightFilters;
import com.sumirelabs.pulsar.api.ColoredTranslucency;
import com.sumirelabs.pulsar.api.PackedLightColor;
import com.sumirelabs.pulsar.api.PositionalColoredTranslucency;
import com.sumirelabs.pulsar.rgb.RgbDefaultPalette;
import github.kasuminova.novaeng.common.performance.PulsarRgbTables;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mixin(value = BlockLightFilters.class, remap = false)
public abstract class MixinBlockLightFilters {
    @Unique
    private static final Reference2IntOpenHashMap<IBlockState> novaeng$filters = novaeng$newFilterTable();

    @Redirect(method = "<clinit>", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC,
        target = "Lcom/sumirelabs/pulsar/api/BlockLightFilters;FILTERS:Ljava/util/Map;"), require = 1)
    private static void novaeng$skipBoxedTable(final Map<IBlockState, Integer> unused) {
        // All registration methods now use the primitive table.
    }

    @Unique
    private static Reference2IntOpenHashMap<IBlockState> novaeng$newFilterTable() {
        // One state per glass color, plus sixteen connection states per pane color.
        final var filters = new Reference2IntOpenHashMap<IBlockState>(16 + 16 * 16);
        filters.defaultReturnValue(PulsarRgbTables.MISSING);
        return filters;
    }

    /**
     * @author NovaEngineering
     * @reason Register packed absorption without boxing and preserve reference identity.
     */
    @Overwrite
    public static synchronized void register(final IBlockState state, final int red, final int green, final int blue) {
        if (PulsarRgbTables.filtersFrozen())
            throw new IllegalStateException("RGB filters are frozen after initialization");
        if (state.getBlock() == Blocks.AIR) throw new IllegalArgumentException("Air cannot filter RGB light");
        novaeng$putFilter(state, PackedLightColor.pack(red, green, blue));
    }

    /**
     * @author NovaEngineering
     * @reason Register interface defaults with primitive packed values and retain explicit registrations.
     */
    @Overwrite
    public static synchronized void interfaceDefaults() {
        if (PulsarRgbTables.filtersFrozen()) throw new IllegalStateException("RGB filters already frozen");
        for (final IBlockState state : Block.BLOCK_STATE_IDS) {
            if (state.getBlock() instanceof ColoredTranslucency source
                && !(source instanceof PositionalColoredTranslucency)) {
                novaeng$putDefaultFilter(state, PackedLightColor.complement(source.getColoredTransmittance(state)));
            }
        }
    }

    /**
     * @author NovaEngineering
     * @reason Visit each stained glass state once and insert its absorption without boxing.
     */
    @Overwrite
    public static synchronized void defaults() {
        if (PulsarRgbTables.filtersFrozen()) return;
        novaeng$registerGlassDefaults(Blocks.STAINED_GLASS);
        novaeng$registerGlassDefaults(Blocks.STAINED_GLASS_PANE);
    }

    @Unique
    private static void novaeng$registerGlassDefaults(final Block block) {
        for (final IBlockState state : block.getBlockState().getValidStates()) {
            final int meta = state.getBlock().getMetaFromState(state);
            if ((meta & 15) == meta) {
                final int transmittance = RgbDefaultPalette.glassTransmittance(EnumDyeColor.byMetadata(meta));
                novaeng$putDefaultFilter(state, transmittance ^ PackedLightColor.WHITE);
            }
        }
    }

    /**
     * @author NovaEngineering
     * @reason Publish the primitive table once every writer has finished.
     */
    @Overwrite
    public static synchronized void freeze() {
        PulsarRgbTables.freezeFilters(novaeng$filters);
    }

    /**
     * @author NovaEngineering
     * @reason Register a whole block with one validation of the packed absorption.
     */
    @Overwrite
    public static synchronized void registerAll(final Block block, final int absorption) {
        if (PulsarRgbTables.filtersFrozen())
            throw new IllegalStateException("RGB filters are frozen after initialization");
        Objects.requireNonNull(block, "block");
        if (block == Blocks.AIR) throw new IllegalArgumentException("Air cannot filter RGB light");
        PackedLightColor.checked(absorption);
        final var states = block.getBlockState().getValidStates();
        if (PulsarRgbTables.filtersFrozen()) return;
        for (int i = 0, count = states.size(); i < count; ++i) {
            novaeng$filters.put(states.get(i), absorption);
        }
    }

    /**
     * @author NovaEngineering
     * @reason Emit filter profile rows without boxing or retaining entry objects.
     */
    @Overwrite
    static void appendProfile(final List<String> rows) {
        if (!PulsarRgbTables.filtersFrozen()) throw new IllegalStateException("RGB filters not initialized");
        PulsarRgbTables.appendFilterRows(rows, Map.of());
    }

    // Registration can call mod code while holding the reentrant class monitor.
    @Unique
    private static void novaeng$putFilter(final IBlockState state, final int value) {
        if (!PulsarRgbTables.filtersFrozen()) novaeng$filters.put(state, value);
    }

    @Unique
    private static void novaeng$putDefaultFilter(final IBlockState state, final int value) {
        if (!PulsarRgbTables.filtersFrozen()) novaeng$filters.putIfAbsent(state, value);
    }
}
