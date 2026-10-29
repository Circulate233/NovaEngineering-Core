package github.kasuminova.novaeng.common.performance;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.BlockStone;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BlockMetadataStatesTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Operation<IBlockState> operation(final AtomicInteger calls) {
        return args -> {
            calls.incrementAndGet();
            return ((IBlockState) args[0]).withProperty((IProperty) args[1], (Comparable) args[2]);
        };
    }

    static Class<?> wrapperClass() throws Exception {
        final String name = "github.kasuminova.novaeng.mixin.minecraft.MixinBlockStoneMetadata";
        final ClassNode node = new ClassNode();
        try (final InputStream stream = BlockMetadataStatesTest.class.getClassLoader()
                                                                     .getResourceAsStream(name.replace('.', '/') + ".class")) {
            assertNotNull(stream);
            new ClassReader(stream).accept(node, 0);
        }
        node.access &= ~Opcodes.ACC_ABSTRACT;
        final ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        final byte[] bytes = writer.toByteArray();
        return new ClassLoader(BlockMetadataStatesTest.class.getClassLoader()) {
            Class<?> define() {
                return this.defineClass(name, bytes, 0, bytes.length);
            }
        }.define();
    }

    static Method select(final Class<?> wrapper) throws Exception {
        final Method select = wrapper.getDeclaredMethod("nova$canonicalMetadataState", IBlockState.class,
            IProperty.class, Comparable.class, Operation.class);
        select.setAccessible(true);
        return select;
    }

    @Test
    void actualWrapperMatchesEveryMetadataAndAvoidsRepeatedPropertyLookups() throws Exception {
        final Class<?> wrapper = wrapperClass();
        final Object instance = wrapper.getDeclaredConstructor().newInstance();
        final Method select = select(wrapper);
        final BlockStone stone = WorldgenTestBlocks.stone();
        final AtomicInteger calls = new AtomicInteger();
        final Operation<IBlockState> original = operation(calls);
        for (int pass = 0; pass < 4; pass++) {
            for (int meta = -5; meta < 20; meta++) {
                final BlockStone.EnumType variant = BlockStone.EnumType.byMetadata(meta);
                final IBlockState actual = (IBlockState) select.invoke(instance, stone.getDefaultState(),
                    BlockStone.VARIANT, variant, original);
                assertSame(stone.getStateFromMeta(meta), actual);
            }
        }
        assertEquals(7, calls.get());
        for (final int meta : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertSame(stone.getStateFromMeta(meta), select.invoke(instance, stone.getDefaultState(),
                BlockStone.VARIANT, BlockStone.EnumType.byMetadata(meta), original));
        }
    }

    @Test
    void changingDefaultIdentityOrBlockInstanceCannotReturnAnOldTransition() throws Exception {
        final Class<?> wrapper = wrapperClass();
        final Object instance = wrapper.getDeclaredConstructor().newInstance();
        final Method select = select(wrapper);
        final AtomicInteger calls = new AtomicInteger();
        final Operation<IBlockState> original = operation(calls);
        final BlockStone first = WorldgenTestBlocks.stone();
        final BlockStone second = WorldgenTestBlocks.stone();
        for (final IBlockState base : new IBlockState[]{first.getDefaultState(),
            first.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE),
            second.getDefaultState(), first.getDefaultState()}) {
            for (int repeat = 0; repeat < 2; repeat++) {
                assertSame(base.withProperty(BlockStone.VARIANT, BlockStone.EnumType.DIORITE),
                    select.invoke(instance, base, BlockStone.VARIANT, BlockStone.EnumType.DIORITE, original));
            }
        }
        assertEquals(4, calls.get());
    }
}
