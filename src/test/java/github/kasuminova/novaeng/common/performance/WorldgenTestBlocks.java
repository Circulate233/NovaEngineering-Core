package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Build real canonical states without bootstrapping unrelated game registries or sounds.
 */
final class WorldgenTestBlocks {

    private WorldgenTestBlocks() {
    }

    static <T> T allocate(final Class<T> type) throws Exception {
        final Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    static <T extends Block> T states(final T block, final IProperty<?>... properties) throws Exception {
        final BlockStateContainer container = new BlockStateContainer(block, properties);
        set(block, "blockState", container);
        set(block, "defaultBlockState", container.getBaseState());
        set(block, "material", Material.ROCK);
        return block;
    }

    static BlockStone stone() throws Exception {
        final BlockStone stone = states(allocate(BlockStone.class), BlockStone.VARIANT);
        set(stone, "defaultBlockState", stone.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.STONE));
        return stone;
    }

    private static void set(final Block block, final String name, final Object value) throws Exception {
        final Field field = Block.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(block, value);
    }
}
