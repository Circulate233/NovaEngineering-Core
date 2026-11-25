package github.kasuminova.novaeng.mixin.pulsar;

import com.sumirelabs.pulsar.api.PackedLightColor;
import com.sumirelabs.pulsar.api.PositionalColoredLightSource;
import com.sumirelabs.pulsar.api.PositionalColoredTranslucency;
import com.sumirelabs.pulsar.api.RgbStateProperties;
import com.sumirelabs.pulsar.rgb.RgbPaletteOverrides;
import github.kasuminova.novaeng.common.performance.PulsarRgbTables;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = RgbStateProperties.class, remap = false)
public abstract class MixinRgbStateProperties {
    /**
     * @author NovaEngineering
     * @reason Read each frozen registry once, without boxing packed colors or filters.
     */
    @Overwrite
    public static int compute(final IBlockState state, final RgbPaletteOverrides.State palette) {
        final int registeredColor = PulsarRgbTables.color(state);
        final int registeredFilter = PulsarRgbTables.filter(state);
        final Integer configuredColor = palette.values.colors().get(state);
        final Integer configuredFilter = palette.values.filters().get(state);
        final int color = configuredColor != null ? configuredColor.intValue() : registeredColor;
        final int filter = configuredFilter != null ? configuredFilter.intValue() : registeredFilter;
        final boolean dynamicSource = color < 0 && state.getBlock() instanceof PositionalColoredLightSource;
        final boolean dynamicFilter = filter < 0 && state.getBlock() instanceof PositionalColoredTranslucency;
        final boolean absolute = dynamicSource || color >= 0 && (color & PulsarRgbTables.ABSOLUTE) != 0;
        return RgbStateProperties.pack(color < 0 ? PackedLightColor.WHITE : color & PackedLightColor.WHITE,
            filter < 0 ? 0 : filter, registeredColor != PulsarRgbTables.UNFROZEN, registeredFilter != PulsarRgbTables.UNFROZEN)
            | (filter >= 0 ? RgbStateProperties.FILTER_PRESENT : 0)
            | (dynamicSource ? RgbStateProperties.DYNAMIC_SOURCE : 0)
            | (absolute ? RgbStateProperties.SOURCE_ABSOLUTE : 0)
            | (dynamicFilter ? RgbStateProperties.DYNAMIC_FILTER : 0);
    }

    /**
     * @author NovaEngineering
     * @reason Check primitive table publication without exposing a mutable map or encoded color flags.
     */
    @Overwrite
    public static boolean hasDynamicStates() {
        final var palette = RgbPaletteOverrides.current();
        final Boolean cached = palette.dynamicStates;
        if (cached != null) return cached;
        if (!PulsarRgbTables.colorsFrozen() || !PulsarRgbTables.filtersFrozen()) return true;
        for (final IBlockState state : Block.BLOCK_STATE_IDS) {
            if ((RgbStateProperties.compute(state, palette) & RgbStateProperties.DYNAMIC) != 0) {
                palette.dynamicStates = true;
                return true;
            }
        }
        palette.dynamicStates = false;
        return false;
    }
}
