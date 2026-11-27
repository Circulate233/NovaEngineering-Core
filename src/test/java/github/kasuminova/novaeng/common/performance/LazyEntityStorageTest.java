package github.kasuminova.novaeng.common.performance;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.world.IBlockAccess;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LazyEntityStorageTest {
    private static final String MIXINS = "github/kasuminova/novaeng/mixin/minecraft/";

    private static Object field(final Object value, final String name) throws Exception {
        final Field field = value.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(value);
    }

    /**
     * Executes the production handlers against the dependency's original method bodies.
     */
    private static ClassNode vanilla(final String name) throws Exception {
        final ClassNode node = new ClassNode();
        try (final var stream = LazyEntityStorageTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(stream);
            new ClassReader(stream).accept(node, 0);
        }
        return node;
    }

    private static List<MethodNode> handlers(final ClassNode target, final String mixin) throws Exception {
        final ClassNode additions = new ClassNode();
        try (final var stream = LazyEntityStorageTest.class.getResourceAsStream("/" + MIXINS + mixin + ".class")) {
            assertNotNull(stream);
            new ClassReader(stream).accept(new ClassRemapper(additions,
                new SimpleRemapper(MIXINS + mixin, target.name)), 0);
        }
        // Mixin @Shadow/@Mutable fields are declarations only. Apply the mutable
        // access change to the original fixture field before defining the probe.
        additions.fields.stream().filter(field -> (field.visibleAnnotations != null && field.visibleAnnotations.stream()
                                                                                                               .anyMatch(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mutable;")))
                     || (field.invisibleAnnotations != null && field.invisibleAnnotations.stream()
                                                                                         .anyMatch(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mutable;"))))
                        .forEach(field -> target.fields.stream().filter(original -> original.name.equals(field.name))
                                                       .forEach(original -> original.access &= ~Opcodes.ACC_FINAL));
        final List<MethodNode> handlers = additions.methods.stream().filter(method -> method.name.startsWith("nova$")).toList();
        target.methods.addAll(handlers);
        return handlers;
    }

    private static MethodNode method(final ClassNode node, final String name) {
        return node.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }

    private static MethodNode handler(final List<MethodNode> handlers, final String name) {
        return handlers.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }

    private static void replaceConstructorCapacity(final ClassNode node, final int capacity) {
        int replaced = 0;
        for (final AbstractInsnNode instruction : method(node, "<init>").instructions) {
            if (instruction instanceof IntInsnNode integer && integer.operand == capacity) {
                integer.operand = 0;
                replaced++;
            }
        }
        assertEquals(1, replaced);
    }

    private static void prepend(final ClassNode target, final MethodNode method, final MethodNode handler) {
        final InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int slot = 1;
        for (final Type argument : Type.getArgumentTypes(method.desc)) {
            call.add(new VarInsnNode(argument.getOpcode(Opcodes.ILOAD), slot));
            slot += argument.getSize();
        }
        call.add(new InsnNode(Opcodes.ACONST_NULL));
        call.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, target.name, handler.name, handler.desc, false));
        method.instructions.insert(call);
    }

    private static Class<?> define(final ClassNode node) {
        final String probeName = "github/kasuminova/novaeng/common/performance/probe/" + node.name.substring(node.name.lastIndexOf('/') + 1);
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(new ClassRemapper(writer, new SimpleRemapper(node.name, probeName)));
        final byte[] bytes = writer.toByteArray();
        return new ClassLoader(LazyEntityStorageTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }

    @Test
    void modifierIndicesAllocateIndependentlyAndRetainEventsAndLiveOperationCollections() throws Exception {
        final ClassNode node = vanilla("net/minecraft/entity/ai/attributes/ModifiableAttributeInstance");
        final List<MethodNode> handlers = handlers(node, "MixinLazyAttributeModifierMaps");
        final MethodNode constructor = method(node, "<init>");
        int ordinal = 0;
        for (final AbstractInsnNode instruction : constructor.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("com/google/common/collect/Maps")
                && call.name.equals("newHashMap")) {
                if (++ordinal > 1) {
                    final String handler = ordinal == 2 ? "nova$emptyNameIndex" : "nova$emptyUuidIndex";
                    constructor.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 0));
                    constructor.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESPECIAL, node.name, handler,
                        "()Ljava/util/HashMap;", false));
                }
            }
        }
        assertEquals(3, ordinal);
        prepend(node, method(node, "applyModifier"), handler(handlers, "nova$allocateModifierIndices"));
        final Class<?> probe = define(node);
        final CountingAttributes owner = new CountingAttributes();
        final IAttribute attribute = new RangedAttribute(null, "test.value", 4, -1000, 1000);
        final IAttributeInstance first = (IAttributeInstance) probe.getConstructor(AbstractAttributeMap.class, IAttribute.class)
                                                                   .newInstance(owner, attribute);
        final IAttributeInstance second = (IAttributeInstance) probe.getConstructor(AbstractAttributeMap.class, IAttribute.class)
                                                                    .newInstance(owner, attribute);
        assertSame(SharedEmptyHashMap.instance(), field(first, "mapByName"));
        assertSame(field(first, "mapByUUID"), field(second, "mapByUUID"));
        assertNull(first.getModifier(UUID.randomUUID()));
        final AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "test.modifier", 3, 0);
        first.removeModifier(modifier);
        assertEquals(1, owner.changes, "Missing removals still notify the owner");
        assertSame(SharedEmptyHashMap.instance(), field(first, "mapByUUID"));
        first.applyModifier(modifier);
        assertEquals(2, owner.changes);
        assertSame(modifier, first.getModifier(modifier.getID()));
        assertEquals(7, first.getAttributeValue());
        assertNotSame(field(first, "mapByUUID"), field(second, "mapByUUID"));
        assertThrows(IllegalArgumentException.class, () -> first.applyModifier(modifier));
        assertEquals(2, owner.changes);
        first.getModifiers().clear();
        assertTrue(first.hasModifier(modifier), "Public modifier snapshots remain independently writable");
        first.removeModifier(modifier);
        assertEquals(3, owner.changes);
        assertEquals(4, first.getAttributeValue());
        final AttributeModifier directlyAdded = new AttributeModifier(UUID.randomUUID(), "direct", 1, 1);
        first.getModifiersByOperation(1).add(directlyAdded);
        assertTrue(first.getModifiersByOperation(1).contains(directlyAdded));
        assertTrue(second.getModifiers().isEmpty());
    }

    @Test
    void heapDefersStorageAndRetainsGrowthOrderingAndIndexUpdates() throws Exception {
        final ClassNode node = vanilla("net/minecraft/pathfinding/PathHeap");
        final List<MethodNode> handlers = handlers(node, "MixinLazyPathHeap");
        replaceConstructorCapacity(node, 128);
        prepend(node, method(node, "addPoint"), handler(handlers, "nova$allocateHeapArray"));
        final Class<?> probe = define(node);
        final Object heap = probe.getConstructor().newInstance();
        assertEquals(0, ((PathPoint[]) field(heap, "pathPoints")).length);
        probe.getMethod("clearPath").invoke(heap);
        assertEquals(0, ((PathPoint[]) field(heap, "pathPoints")).length);
        final List<PathPoint> points = new ObjectArrayList<>();
        for (int index = 299; index >= 0; index--) {
            final PathPoint point = new PathPoint(index, 0, 0);
            point.distanceToTarget = index;
            points.add(point);
            probe.getMethod("addPoint", PathPoint.class).invoke(heap, point);
        }
        assertEquals(512, ((PathPoint[]) field(heap, "pathPoints")).length);
        final PathPoint changed = points.getFirst();
        probe.getMethod("changeDistance", PathPoint.class, float.class).invoke(heap, changed, -1F);
        assertSame(changed, probe.getMethod("dequeue").invoke(heap));
        assertEquals(-1, changed.index);
        for (int index = 0; index < 299; index++) {
            final PathPoint point = (PathPoint) probe.getMethod("dequeue").invoke(heap);
            assertEquals(index, point.x);
            assertEquals(-1, point.index);
        }
        assertEquals(true, probe.getMethod("isPathEmpty").invoke(heap));
    }

    @Test
    void pathOptionsKeepOneWritableArrayAcrossSearches() throws Exception {
        final ClassNode node = vanilla("net/minecraft/pathfinding/PathFinder");
        final List<MethodNode> handlers = handlers(node, "MixinLazyPathFinder");
        replaceConstructorCapacity(node, 32);
        final MethodNode search = node.methods.stream().filter(method -> method.name.equals("findPath")
            && method.desc.startsWith("(Lnet/minecraft/pathfinding/PathPoint;")).findFirst().orElseThrow();
        prepend(node, search, handler(handlers, "nova$allocateOptionsArray"));
        final Class<?> probe = define(node);
        final RecordingNodes processor = new RecordingNodes();
        final Object finder = probe.getConstructor(NodeProcessor.class).newInstance(processor);
        assertEquals(0, ((PathPoint[]) field(finder, "pathOptions")).length);
        final var find = probe.getDeclaredMethod("findPath", PathPoint.class, PathPoint.class, float.class);
        find.setAccessible(true);
        final Path first = (Path) find.invoke(finder, new PathPoint(0, 0, 0), new PathPoint(2, 0, 0), 10F);
        assertEquals(3, first.getCurrentPathLength());
        final PathPoint[] backing = (PathPoint[]) field(finder, "pathOptions");
        assertEquals(32, backing.length);
        assertSame(backing, processor.options);
        find.invoke(finder, new PathPoint(0, 0, 0), new PathPoint(2, 0, 0), 10F);
        assertSame(backing, field(finder, "pathOptions"));
        assertSame(backing, processor.options);
    }

    @Test
    void sharedEmptyIndexCannotBeContaminated() {
        final HashMap<String, String> empty = SharedEmptyHashMap.instance();
        assertNull(empty.remove("missing"));
        assertFalse(empty.remove("missing", "value"));
        empty.clear();
        assertThrows(UnsupportedOperationException.class, () -> empty.put("key", "value"));
        assertThrows(UnsupportedOperationException.class, () -> empty.putIfAbsent("key", "value"));
        assertThrows(UnsupportedOperationException.class, () -> empty.computeIfAbsent("key", key -> "value"));
        assertThrows(UnsupportedOperationException.class, () -> empty.compute("key", (key, value) -> "value"));
        assertThrows(UnsupportedOperationException.class, () -> empty.merge("key", "value", (a, b) -> a));
        assertTrue(empty.isEmpty());
    }

    private static final class CountingAttributes extends AbstractAttributeMap {
        int changes;

        @Override
        protected IAttributeInstance createInstance(final IAttribute attribute) {
            return new ModifiableAttributeInstance(this, attribute);
        }

        @Override
        public void onAttributeModified(final IAttributeInstance instance) {
            changes++;
        }
    }

    private static final class RecordingNodes extends NodeProcessor {
        PathPoint[] options;

        @Override
        public PathPoint getStart() {
            return new PathPoint(0, 0, 0);
        }

        @Override
        public PathPoint getPathPointToCoords(final double x, final double y, final double z) {
            return new PathPoint((int) x, (int) y, (int) z);
        }

        @Override
        public int findPathOptions(final PathPoint[] options, final PathPoint current, final PathPoint target,
                                   final float maxDistance) {
            if (this.options != null) assertSame(this.options, options);
            this.options = options;
            options[0] = current.x == 0 ? new PathPoint(1, 0, 0) : target;
            return 1;
        }

        @Override
        public PathNodeType getPathNodeType(final IBlockAccess world, final int x, final int y, final int z,
                                            final EntityLiving entity, final int sx, final int sy, final int sz,
                                            final boolean breakDoors, final boolean enterDoors) {
            return PathNodeType.WALKABLE;
        }

        @Override
        public PathNodeType getPathNodeType(final IBlockAccess world, final int x, final int y, final int z) {
            return PathNodeType.WALKABLE;
        }
    }
}
