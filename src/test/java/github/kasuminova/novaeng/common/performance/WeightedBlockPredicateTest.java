package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedBlockPredicateTest {

    @Test
    void actualMaterialMixinKeepsItsImmutableMatcherAndNullSemantics() throws Exception {
        final Class<?> type = new WorldgenFixtureLoader(true).loadClass("cofh.cofhworld.util.random.WeightedBlock");
        final Object material = type.getConstructor(Block.class).newInstance((Object) null);
        final WeightedBlockPredicate access = (WeightedBlockPredicate) material;
        final BlockMatcher first = access.nova$replacementPredicate();
        assertSame(first, access.nova$replacementPredicate());
        assertFalse(first.apply(null));
        final IBlockState state = (IBlockState) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{IBlockState.class}, (proxy, method, args) -> null);
        assertEquals(BlockMatcher.forBlock(null).apply(state), first.apply(state));
        final Object second = type.getConstructor(Block.class).newInstance((Object) null);
        assertNotSame(first, ((WeightedBlockPredicate) second).nova$replacementPredicate());
    }

    @Test
    void dependencyHasOneMatcherCallAndAnUnambiguousMaterialLocal() throws Exception {
        final ClassNode node = new ClassNode();
        try (final InputStream stream = getClass().getClassLoader()
                                                  .getResourceAsStream("cofh/cofhworld/world/generator/WorldGen.class")) {
            assertNotNull(stream);
            new ClassReader(stream).accept(node, 0);
        }
        final MethodNode method = node.methods.stream().filter(candidate -> candidate.name.equals("canGenerateInBlock")
            && candidate.desc.contains("BlockPos;")).findFirst().orElseThrow();
        int calls = 0;
        for (final AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("net/minecraft/block/state/pattern/BlockMatcher")
                && call.name.equals("forBlock")) {
                calls++;
            }
        }
        assertEquals(1, calls);
        assertTrue(method.localVariables.stream().anyMatch(local -> local.desc.equals("Lcofh/cofhworld/util/random/WeightedBlock;")));
    }
}
