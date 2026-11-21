package github.kasuminova.novaeng.common.hypernet.old.research;

import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResearchStationTypeTest {
    private static final BlockPos POS = new BlockPos(1, 2, 3);

    private static TestController controller(final Float value) throws Exception {
        TestController result = allocate(TestController.class);
        result.data = new NBTTagCompound();
        if (value != null) {
            result.interfaceData = new SmartInterfaceData(POS, new ResourceLocation("novaeng", "research"), "overclocking", value);
        }
        return result;
    }

    private static <T> T allocate(final Class<T> type) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        var unsafeField = unsafeType.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        return type.cast(unsafeType.getMethod("allocateInstance", Class.class).invoke(unsafe, type));
    }

    @Test
    void normalizeClampsFiniteValuesToHardwareRange() {
        assertEquals((short) 1, ResearchStationType.normalizeOverclocking(0F));
        assertEquals((short) 1, ResearchStationType.normalizeOverclocking(1F));
        assertEquals((short) 3, ResearchStationType.normalizeOverclocking(3.9F));
        assertEquals((short) 5, ResearchStationType.normalizeOverclocking(5F));
        assertEquals((short) 5, ResearchStationType.normalizeOverclocking(6F));
        assertEquals((short) 5, ResearchStationType.normalizeOverclocking(32_767F));
        assertEquals((short) 5, ResearchStationType.normalizeOverclocking(65_540F));
    }

    @Test
    void normalizeHandlesNonFiniteValuesWithoutShortWraparound() {
        assertEquals((short) 1, ResearchStationType.normalizeOverclocking(Float.NaN));
        assertEquals((short) 5, ResearchStationType.normalizeOverclocking(Float.POSITIVE_INFINITY));
        assertEquals((short) 1, ResearchStationType.normalizeOverclocking(Float.NEGATIVE_INFINITY));
    }

    @Test
    void postTickClampsInterfaceAndWritesTheSameValueToNbt() throws Exception {
        for (float input : new float[]{6F, 32_767F, 65_540F, Float.NaN, Float.POSITIVE_INFINITY}) {
            TestController controller = controller(input);
            ResearchStationType.updateOverclocking(controller);

            short expected = ResearchStationType.normalizeOverclocking(input);
            assertEquals(expected, controller.getCustomDataTag().getShort("overclocking"), "NBT for " + input);
            assertEquals(expected, controller.interfaceData.getValue(), "interface for " + input);
        }
    }

    @Test
    void missingInterfaceDefaultsToOneAndDoesNotCreateClientOnlyState() throws Exception {
        TestController controller = controller(null);
        ResearchStationType.updateOverclocking(controller);

        assertEquals((short) 1, controller.getCustomDataTag().getShort("overclocking"));
        assertNull(controller.interfaceData);
    }

    private static final class TestController extends TileFactoryController {
        private NBTTagCompound data;
        private SmartInterfaceData interfaceData;

        @Override
        public NBTTagCompound getCustomDataTag() {
            return data;
        }

        @Override
        public SmartInterfaceData getSmartInterfaceData(final String type) {
            return "overclocking".equals(type) ? interfaceData : null;
        }
    }
}
