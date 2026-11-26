package github.kasuminova.novaeng.mixin.enderio;

import com.enderio.core.client.render.BoundingBox;
import github.kasuminova.novaeng.common.performance.ConduitComponentBounds;
import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import net.minecraft.util.math.AxisAlignedBB;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConduitBoundsMixinTest {
    private static final String BLOCK = "crazypants/enderio/conduits/conduit/BlockConduitBundle";
    private static final String COMPONENT = "crazypants/enderio/base/conduit/geom/CollidableComponent";

    @Test
    void geometryCacheHasComponentLifetimeAndAlwaysReturnsAnOrdinaryBox() {
        final var first = new MixinCollidableComponent() { };
        first.bound = new BoundingBox(.125, .25, .375, .5, .75, .875);
        final var second = new MixinCollidableComponent() { };
        second.bound = new BoundingBox(.125, .25, .375, .5, .75, .875);
        final AxisAlignedBB retained = first.nova$ordinaryCollisionBounds();
        assertEquals(AxisAlignedBB.class, retained.getClass());
        assertSame(retained, first.nova$ordinaryCollisionBounds());
        assertNotSame(retained, second.nova$ordinaryCollisionBounds());
        retained.offset(10, 20, 30);
        assertEquals(.125, retained.minX);
        assertEquals(.875, retained.maxZ);
    }

    @Test
    void actualEnderIoTargetsAcceptBothInjectorsAndKeepEveryCollisionCallback() throws Exception {
        final ClassNode before = resource(BLOCK);
        final byte[][] transformed = IsolatedMixinTransformer.transformInPackage(
            "github.kasuminova.novaeng.mixin.enderio", new String[]{COMPONENT, BLOCK},
            "MixinCollidableComponent", "MixinBlockConduitBundleBounds");
        final ClassNode component = read(transformed[0]);
        assertTrue(component.interfaces.contains(ConduitComponentBounds.class.getName().replace('.', '/')));
        assertTrue(component.methods.stream().anyMatch(m -> m.name.equals("nova$ordinaryCollisionBounds")));
        final ClassNode block = read(transformed[1]);
        final MethodNode originalCollision = method(before, "addCollisionBoxToList");
        final MethodNode collision = method(block, "addCollisionBoxToList");
        assertEquals(4, calls(originalCollision, BLOCK, "setBlockBounds"));
        assertEquals(3, calls(collision, BLOCK, "setBlockBounds"));
        assertEquals(3, calls(collision, before.superName, "addCollisionBoxToList"));
        assertEquals(calls(originalCollision, before.superName, "addCollisionBoxToList"),
            calls(collision, before.superName, "addCollisionBoxToList"));
        assertTrue(block.methods.stream().anyMatch(m -> m.name.contains("nova$componentBounds")));
        assertEquals(0, allocations(method(block, "setBlockBounds"), "net/minecraft/util/math/AxisAlignedBB"));
    }

    private static ClassNode resource(final String name) throws Exception {
        try (final var in = ConduitBoundsMixinTest.class.getResourceAsStream('/' + name + ".class")) {
            final ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, 0);
            return node;
        }
    }

    private static ClassNode read(final byte[] bytes) {
        final ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static MethodNode method(final ClassNode node, final String name) {
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }

    private static int calls(final MethodNode method, final String owner, final String name) {
        int count = 0;
        for (final var insn : method.instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) count++;
        }
        return count;
    }

    private static int allocations(final MethodNode method, final String type) {
        int count = 0;
        for (final var insn : method.instructions) {
            if (insn instanceof TypeInsnNode allocation && allocation.getOpcode() == 187
                && allocation.desc.equals(type)) count++;
        }
        return count;
    }
}
