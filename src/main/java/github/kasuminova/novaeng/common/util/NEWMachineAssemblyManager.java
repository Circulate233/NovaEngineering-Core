package github.kasuminova.novaeng.common.util;

import github.kasuminova.novaeng.NovaEngineeringCore;
import hellfirepvp.modularmachinery.common.integration.ModIntegrationJEI;
import hellfirepvp.modularmachinery.common.integration.preview.StructurePreviewWrapper;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class NEWMachineAssemblyManager {
    private static final Reference2ObjectOpenHashMap<Block, Int2ObjectOpenHashMap<DynamicMachine>> CONSTRUCTORS = new Reference2ObjectOpenHashMap<>();

    private NEWMachineAssemblyManager() {
    }

    public static Collection<DynamicMachine> getAllDynamicMachines() {
        final List<DynamicMachine> result = new ObjectArrayList<>();
        for (final var map : CONSTRUCTORS.values()) {
            result.addAll(map.values());
        }
        return result;
    }

    @Nullable
    public static DynamicMachine getDynamicMachine(final IBlockState state) {
        return getDynamicMachine(state.getBlock(), state.getBlock().getMetaFromState(state));
    }

    @Nullable
    public static DynamicMachine getDynamicMachine(final Block block, final int meta) {
        final var map = CONSTRUCTORS.get(block);
        return map == null ? null : map.get(meta);
    }

    public static ObjectIterator<Map.Entry<Block, Int2ObjectOpenHashMap<DynamicMachine>>> getConstructorsIterator() {
        return CONSTRUCTORS.entrySet().iterator();
    }

    public static void setConstructors(final BlockPair block, final NEWDynamicMachine machine) {
        CONSTRUCTORS.computeIfAbsent(block.block(), ignored -> new Int2ObjectOpenHashMap<>()).put(block.meta(), machine);
        if (NovaEngineeringCore.proxy.isClient()) {
            ModIntegrationJEI.PREVIEW_WRAPPERS.add(new StructurePreviewWrapper(machine));
        }
    }

    public record BlockPair(Block block, int meta) {
        public Block getBlock() {
            return this.block;
        }

        public int getMeta() {
            return this.meta;
        }

        @SuppressWarnings("deprecation")
        public IBlockState getBlockState() {
            return this.block.getStateFromMeta(this.meta);
        }
    }
}
