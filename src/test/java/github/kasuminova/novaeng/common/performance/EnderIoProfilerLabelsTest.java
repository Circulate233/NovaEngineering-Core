package github.kasuminova.novaeng.common.performance;

import crazypants.enderio.util.Profiler;
import github.kasuminova.novaeng.mixin.enderio.AccessorEnderIoProfiler;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnderIoProfilerLabelsTest {
    private static final class LabelBlock extends Block {
        private List<String> calls;
        private String[] currentName;

        private LabelBlock() {
            super(Material.ROCK);
            throw new AssertionError("This label-only probe must not initialize Minecraft block state");
        }

        @Override
        public String getLocalizedName() {
            calls.add("name");
            return currentName[0];
        }
    }

    private static LabelBlock labelBlock(final List<String> calls, final String[] currentName) throws Exception {
        // Only the overridden name reader is used. Avoid Block's instance initialization,
        // which requires Sounds bootstrap and would globally freeze registries for other tests.
        final Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        final LabelBlock block = (LabelBlock) ((Unsafe) field.get(null)).allocateInstance(LabelBlock.class);
        block.calls = calls;
        block.currentName = currentName;
        return block;
    }

    private static final class RecordingProfiler extends Profiler implements AccessorEnderIoProfiler {
        private final boolean enabled;
        private final List<String> calls;

        private RecordingProfiler(final boolean enabled, final List<String> calls) {
            super(enabled);
            this.enabled = enabled;
            this.calls = calls;
        }

        @Override
        public boolean nova$isEnabled() {
            return enabled;
        }

        @Override
        public void stop(final long start, final String label) {
            calls.add("stop:" + start + ":" + label);
        }
    }

    @Test
    void disabledProfilerDoesNotReadStateOrProduceLabels() {
        final List<String> calls = new ArrayList<>();
        EnderIoProfilerLabels.stop(new RecordingProfiler(false, calls), 123, null);
        assertEquals(List.of(), calls);
    }

    @Test
    void enabledProfilerKeepsLabelOrderAndObservesChangingNames() throws Exception {
        final List<String> calls = new ArrayList<>();
        final String[] currentName = {"first"};
        final Block block = labelBlock(calls, currentName);
        final IBlockState state = (IBlockState) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{IBlockState.class}, (proxy, method, arguments) -> {
                if (!method.getName().equals("getBlock")) throw new AssertionError(method);
                calls.add("block");
                return block;
            });
        final RecordingProfiler profiler = new RecordingProfiler(true, calls);
        EnderIoProfilerLabels.stop(profiler, 123, state);
        currentName[0] = "after reload";
        EnderIoProfilerLabels.stop(profiler, 456, state);
        assertEquals(List.of("block", "name", "stop:123:first (relayed)",
            "block", "name", "stop:456:after reload (relayed)"), calls);
    }
}
