package github.kasuminova.novaeng.mixin.pulsar;

import com.sumirelabs.pulsar.api.BlockLightColors;
import com.sumirelabs.pulsar.api.BlockLightFilters;
import com.sumirelabs.pulsar.api.ColoredLightSource;
import com.sumirelabs.pulsar.api.PackedLightColor;
import com.sumirelabs.pulsar.api.PositionalColoredLightSource;
import com.sumirelabs.pulsar.api.RgbStateProperties;
import com.sumirelabs.pulsar.compat.ModLightColors;
import com.sumirelabs.pulsar.rgb.RgbLight;
import com.sumirelabs.pulsar.rgb.RgbPaletteOverrides;
import com.sumirelabs.pulsar.rgb.RgbSettings;
import github.kasuminova.novaeng.common.performance.PulsarRgbTables;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Mixin(value = BlockLightColors.class, remap = false)
public abstract class MixinBlockLightColors {
    @Unique
    private static final Reference2IntOpenHashMap<IBlockState> novaeng$colors = novaeng$newColorTable();

    @Redirect(method = "<clinit>", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC,
        target = "Lcom/sumirelabs/pulsar/api/BlockLightColors;COLORS:Ljava/util/Map;"), require = 1)
    private static void novaeng$skipBoxedTable(final Map<IBlockState, Integer> unused) {
        // All registration methods now use the primitive table.
    }

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE",
        target = "Ljava/util/Collections;newSetFromMap(Ljava/util/Map;)Ljava/util/Set;"), require = 1)
    private static Set<IBlockState> novaeng$skipAbsoluteSet(final Map<IBlockState, Boolean> unused) {
        // Absolute emission is stored in the same value as the color.
        return null;
    }

    @Unique
    private static Reference2IntOpenHashMap<IBlockState> novaeng$newColorTable() {
        final var colors = new Reference2IntOpenHashMap<IBlockState>(128);
        colors.defaultReturnValue(PulsarRgbTables.MISSING);
        return colors;
    }

    @Unique
    private static void novaeng$checkRegistration(final IBlockState state) {
        if (PulsarRgbTables.colorsFrozen())
            throw new IllegalStateException("RGB colors are frozen after initialization");
        if (state.getBlock() == Blocks.AIR) throw new IllegalArgumentException("Air cannot be an RGB source");
    }

    /**
     * @author NovaEngineering
     * @reason Store the normalized hue as a primitive int.
     */
    @Overwrite
    public static synchronized void register(final IBlockState state, final int red, final int green, final int blue) {
        novaeng$checkRegistration(state);
        novaeng$putColor(state, RgbLight.normalize(PackedLightColor.pack(red, green, blue)));
    }

    /**
     * @author NovaEngineering
     * @reason Store absolute emission and its flag together.
     */
    @Overwrite
    public static synchronized void registerEmission(final IBlockState state, final int red, final int green,
                                                     final int blue) {
        novaeng$checkRegistration(state);
        novaeng$putColor(state, PackedLightColor.pack(red, green, blue) | PulsarRgbTables.ABSOLUTE);
    }

    /**
     * @author NovaEngineering
     * @reason Store explicit disabled emission without a boxed value or separate flag table.
     */
    @Overwrite
    public static synchronized void disable(final IBlockState state) {
        novaeng$checkRegistration(state);
        novaeng$putColor(state, 0);
    }

    /**
     * @author NovaEngineering
     * @reason Normalize once for all canonical states instead of repeating the per-state API call.
     */
    @Overwrite
    public static synchronized void registerAll(final Block block, final int color) {
        requireMutableBlock(block);
        novaeng$putAll(block, RgbLight.normalize(PackedLightColor.checked(color)));
    }

    /**
     * @author NovaEngineering
     * @reason Validate and encode absolute emission once for the whole block.
     */
    @Overwrite
    public static synchronized void registerEmissionAll(final Block block, final int color) {
        requireMutableBlock(block);
        novaeng$putAll(block, PackedLightColor.checked(color) | PulsarRgbTables.ABSOLUTE);
    }

    /**
     * @author NovaEngineering
     * @reason Write the primitive disabled value directly for every state of the block.
     */
    @Overwrite
    public static synchronized void disableAll(final Block block) {
        requireMutableBlock(block);
        novaeng$putAll(block, 0);
    }

    @Unique
    private static void novaeng$putAll(final Block block, final int color) {
        final var states = block.getBlockState().getValidStates();
        if (PulsarRgbTables.colorsFrozen()) return;
        for (int i = 0, count = states.size(); i < count; ++i) {
            novaeng$colors.put(states.get(i), color);
        }
    }

    /**
     * @author NovaEngineering
     * @reason Read the absolute flag from the frozen color value.
     */
    @Overwrite
    static boolean absolute(final IBlockState state) {
        if (!PulsarRgbTables.colorsFrozen()) return false;
        final int value = novaeng$colors.getInt(state);
        return value >= 0 && (value & PulsarRgbTables.ABSOLUTE) != 0;
    }

    /**
     * @author NovaEngineering
     * @reason Store static interface emission without boxing, while retaining explicit registrations.
     */
    @Overwrite
    public static synchronized void interfaceDefaults() {
        if (PulsarRgbTables.colorsFrozen()) throw new IllegalStateException("RGB colors already frozen");
        for (final IBlockState state : Block.BLOCK_STATE_IDS) {
            if (state.getBlock() instanceof ColoredLightSource source && !(source instanceof PositionalColoredLightSource)
                && !novaeng$colors.containsKey(state)) {
                final int color = PackedLightColor.checked(source.getColoredLightEmission(state));
                novaeng$putColor(state, color | PulsarRgbTables.ABSOLUTE);
            }
        }
    }

    /**
     * @author NovaEngineering
     * @reason Store vanilla hues in the primitive table before publication.
     */
    @Overwrite
    private static void defaultHue(final Block block, final int color) {
        if (PulsarRgbTables.colorsFrozen()) return;
        final int normalized = RgbLight.normalize(color);
        for (final IBlockState state : block.getBlockState().getValidStates()) {
            novaeng$putDefaultColor(state, normalized);
        }
    }

    /**
     * @author NovaEngineering
     * @reason Transfer optional mod defaults into the same primitive registry.
     */
    @Overwrite
    public static synchronized void modDefaults() {
        if (PulsarRgbTables.colorsFrozen()) throw new IllegalStateException("RGB colors already frozen");
        ModLightColors.registerDefaults((state, color) -> novaeng$putDefaultColor(state, RgbLight.normalize(color)));
    }

    /**
     * @author NovaEngineering
     * @reason Publish one table after both registries are ready.
     */
    @Overwrite
    public static synchronized void freeze() {
        BlockLightFilters.freeze();
        PulsarRgbTables.freezeColors(novaeng$colors);
    }

    /**
     * @author NovaEngineering
     * @reason Emit profile rows by iterating the frozen tables and overlay directly.
     */
    @Overwrite
    public static String profile() {
        final var palette = RgbPaletteOverrides.current();
        final String cached = palette.profile;
        if (cached != null) return cached;
        if (!PulsarRgbTables.colorsFrozen()) throw new IllegalStateException("RGB colors not initialized");

        final var rows = new ArrayList<String>();
        rows.add("gameplay:" + RgbSettings.profile());
        PulsarRgbTables.appendColorRows(rows, palette.values.colors());
        PulsarRgbTables.appendFilterRows(rows, palette.values.filters());
        for (final IBlockState state : Block.BLOCK_STATE_IDS) {
            final int dynamic = RgbStateProperties.compute(state, palette) & RgbStateProperties.DYNAMIC;
            if (dynamic != 0)
                rows.add("dynamic:" + state + ":" + state.getBlock().getClass().getName() + "=" + dynamic);
        }
        rows.sort(null);
        try {
            final byte[] hash = MessageDigest.getInstance("SHA-256").digest(String.join("\n", rows).getBytes(StandardCharsets.UTF_8));
            final String profile = HexFormat.of().formatHex(hash);
            palette.profile = profile;
            return profile;
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /**
     * @author NovaEngineering
     * @reason Use the same publication state for public bulk registration.
     */
    @Overwrite
    private static void requireMutableBlock(final Block block) {
        if (PulsarRgbTables.colorsFrozen())
            throw new IllegalStateException("RGB colors are frozen after initialization");
        Objects.requireNonNull(block, "block");
        if (block == Blocks.AIR) throw new IllegalArgumentException("Air cannot be an RGB source");
    }

    // Registration can call mod code while holding the reentrant class monitor.
    @Unique
    private static void novaeng$putColor(final IBlockState state, final int value) {
        if (!PulsarRgbTables.colorsFrozen()) novaeng$colors.put(state, value);
    }

    @Unique
    private static void novaeng$putDefaultColor(final IBlockState state, final int value) {
        if (!PulsarRgbTables.colorsFrozen()) novaeng$colors.putIfAbsent(state, value);
    }
}
