package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.mmce.common.util.BlockPos2ValueMap;
import com.sun.management.ThreadMXBean;
import github.kasuminova.novaeng.common.performance.mmce.OwnedRequirementStack;
import github.kasuminova.novaeng.common.performance.mmce.StructureQueries;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeMachineAllocationTest {
    private static volatile Object sink;
    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    private static Class<?> define(final ClassNode node) {
        final ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        final byte[] bytes = writer.toByteArray();
        return new ClassLoader(RuntimeMachineAllocationTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }

    private static Class<?> ownedConstructor() throws Exception {
        final ClassNode node = new ClassNode();
        final String original = RequirementItem.class.getName().replace('.', '/');
        try (final var stream = RequirementItem.class.getResourceAsStream("RequirementItem.class")) {
            new ClassReader(stream).accept(new ClassRemapper(node,
                new SimpleRemapper(original, original + "OwnedProbe")), 0);
        }
        // Execute the dependency's actual constructor, with exactly the production copy redirect.
        node.methods.removeIf(method -> !method.name.equals("<init>")
            || !method.desc.equals("(Lhellfirepvp/modularmachinery/common/machine/IOType;Lnet/minecraft/item/ItemStack;)V"));
        for (final var insn : node.methods.getFirst().instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals("net/minecraft/item/ItemStack")
                && call.name.equals("copy")) {
                call.setOpcode(Opcodes.INVOKESTATIC);
                call.owner = OwnedRequirementStack.class.getName().replace('.', '/');
                call.name = "copyForConstructor";
                call.desc = "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;";
            }
        }
        return define(node);
    }

    private static ItemStack construct(final Class<?> type, final ItemStack stack) {
        try {
            final Object result = type.getConstructor(IOType.class, ItemStack.class).newInstance(IOType.INPUT, stack);
            return (ItemStack) type.getField("required").get(result);
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    @Test
    void actualRequirementConstructorTransfersOnlyAttemptOwnedStackAndUnwindsScope() throws Exception {
        final Class<?> type = ownedConstructor();
        final ItemStack source = new ItemStack(new Item(), 8);
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("variant", 1);
        source.setTagCompound(tag);
        assertNotSame(source, construct(type, source), "Public constructor still defensively copies");
        final ItemStack owned = source.copy();
        assertNotSame(source, OwnedRequirementStack.construct(owned, () -> construct(type, source)),
            "A scope must not grant ownership of unrelated stacks");
        owned.setCount(3);
        final ItemStack result = OwnedRequirementStack.construct(owned, () -> construct(type, owned));
        assertSame(owned, result);
        assertEquals(3, result.getCount());
        result.getTagCompound().setInteger("variant", 2);
        assertEquals(1, source.getTagCompound().getInteger("variant"));
        assertEquals(8, source.getCount());
        assertNotSame(owned, construct(type, owned));
        owned.setCount(0);
        assertSame(ItemStack.EMPTY, OwnedRequirementStack.construct(owned, () -> construct(type, owned)));
        assertThrows(IllegalStateException.class, () -> OwnedRequirementStack.construct(source, () -> {
            throw new IllegalStateException("failed constructor");
        }));
        assertNotSame(source, construct(type, source));

        try (final var executor = Executors.newFixedThreadPool(4)) {
            final List<Callable<ItemStack>> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) tasks.add(() -> {
                final ItemStack attempt = source.copy();
                return OwnedRequirementStack.construct(attempt, () -> construct(type, attempt));
            });
            final var results = executor.invokeAll(tasks);
            for (int i = 0; i < results.size(); i++) for (int j = 0; j < i; j++) {
                assertNotSame(results.get(i).get(), results.get(j).get());
                assertNotSame(results.get(i).get().getTagCompound(), results.get(j).get().getTagCompound());
            }
        }
    }

    @Test
    void nestedNbtConstructorTransferRemovesTheSecondDeepCopy() throws Exception {
        final Class<?> type = ownedConstructor();
        final ItemStack source = new ItemStack(new Item(), 9);
        final NBTTagCompound root = new NBTTagCompound();
        for (int i = 0; i < 32; i++) {
            final NBTTagCompound child = new NBTTagCompound();
            child.setIntArray("data", new int[128]);
            root.setTag("child" + i, child);
        }
        source.setTagCompound(root);
        for (int i = 0; i < 2000; i++) {
            final ItemStack firstCopy = source.copy();
            sink = construct(type, firstCopy);
            sink = OwnedRequirementStack.construct(firstCopy, () -> construct(type, firstCopy));
        }
        final ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        final long id = Thread.currentThread().threadId();
        final int count = 1000;
        final long before = bean.getThreadAllocatedBytes(id);
        for (int i = 0; i < count; i++) sink = construct(type, source.copy());
        final long original = bean.getThreadAllocatedBytes(id) - before;
        final long ownedBefore = bean.getThreadAllocatedBytes(id);
        for (int i = 0; i < count; i++) {
            final ItemStack firstCopy = source.copy();
            sink = OwnedRequirementStack.construct(firstCopy, () -> construct(type, firstCopy));
        }
        final long owned = bean.getThreadAllocatedBytes(id) - ownedBefore;
        System.out.println("MMCE nested-NBT requirement constructor bytes/op: original="
            + original / (double) count + ", owned=" + owned / (double) count);
        assertTrue(owned < original * 0.7, "One of two large NBT copies must disappear");
    }

    static final class PackedPattern extends BlockPos2ValueMap<BlockArray.BlockInformation>
        implements AccessorBlockPos2ValueMap<BlockArray.BlockInformation> {
        @Override public Long2ObjectMap<BlockArray.BlockInformation> nova$internal() {
            return internal;
        }
    }

    static final class Check extends BlockArray.BlockInformation {
        private final boolean answer;
        private final List<BlockPos> calls;

        Check(final boolean answer, final List<BlockPos> calls) {
            super(List.of());
            this.answer = answer;
            this.calls = calls;
        }

        @Override public boolean matches(final World world, final BlockPos at, final boolean oldState) {
            calls.add(at);
            return answer;
        }
    }

    @Test
    void packedStructureQueriesKeepOriginalOrderFallbackAndImmutableEscapingPositions() throws Exception {
        final PackedPattern pattern = new PackedPattern();
        final List<BlockPos> calls = new ArrayList<>();
        for (int i = -30; i < 30; i++) pattern.put(new BlockPos(i, -i, i * 3), new Check(true, calls));
        final BlockArray original = new BlockArray(123);
        final var field = BlockArray.class.getDeclaredField("pattern");
        field.setAccessible(true);
        field.set(original, pattern);
        final BlockPos center = new BlockPos(400, 80, -200);
        assertTrue(original.matches(null, center, true, null));
        final List<BlockPos> expected = List.copyOf(calls);
        calls.clear();
        assertTrue(StructureQueries.matchesPacked(pattern.nova$internal(), null, center, true, null));
        assertEquals(expected, calls);
        assertTrue(calls.stream().noneMatch(BlockPos.MutableBlockPos.class::isInstance));
        final BlockPos first = pattern.entrySet().iterator().next().getKey();
        pattern.put(first, new Check(false, calls));
        final Map<BlockPos, List<BlockArray.BlockInformation>> replacements = Map.of(first,
            List.of(new Check(false, calls), new Check(true, calls)));
        calls.clear();
        assertTrue(original.matches(null, center, false, replacements));
        final List<BlockPos> withReplacement = List.copyOf(calls);
        calls.clear();
        assertTrue(StructureQueries.matchesPacked(pattern.nova$internal(), null, center, false, replacements));
        assertEquals(withReplacement, calls);
        assertFalse(StructureQueries.matchesPacked(pattern.nova$internal(), null, center, false, null));
        pattern.clear();
        assertTrue(StructureQueries.matchesPacked(pattern.nova$internal(), null, center, false, null));
    }

    @Test
    void lazyModifierAppliersMatchDependencyFloatOrderAndDefaultRetention() throws Exception {
        final ClassNode node = new ClassNode();
        try (final var stream = MixinRecipeModifierAppliers.class.getResourceAsStream("MixinRecipeModifierAppliers.class")) {
            new ClassReader(stream).accept(node, 0);
        }
        node.access &= ~Opcodes.ACC_ABSTRACT;
        final Object optimized = define(node).getConstructor().newInstance();
        final Map<RequirementType<?, ?>, RecipeModifier.ModifierApplier> value = new java.util.HashMap<>();
        final Map<RequirementType<?, ?>, RecipeModifier.ModifierApplier> chance = new java.util.HashMap<>();
        for (final var field : optimized.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            field.set(optimized, field.getName().equals("modifierAppliers") ? value : chance);
        }
        final var method = optimized.getClass().getMethod("addModifierApplier", RequirementType.class, List.class);
        final List<RecipeModifier> mods = new CopyOnWriteArrayList<>(List.of(
            new RecipeModifier(null, IOType.INPUT, 3, 0, false),
            new RecipeModifier(null, IOType.INPUT, 1.2F, 1, false),
            new RecipeModifier(null, IOType.OUTPUT, 0.7F, 1, true)));
        method.invoke(optimized, null, mods);
        final RecipeModifier.ModifierApplier expectedValue = new RecipeModifier.ModifierApplier();
        final RecipeModifier.ModifierApplier expectedChance = new RecipeModifier.ModifierApplier();
        mods.forEach(mod -> RecipeModifier.applyValueToApplier(mod.affectsChance() ? expectedChance : expectedValue, mod));
        for (final IOType io : IOType.values()) {
            assertEquals(expectedValue.apply(8, io), value.get(null).apply(8, io));
            assertEquals(expectedChance.apply(8, io), chance.get(null).apply(8, io));
        }
        final Object retained = value.get(null);
        method.invoke(optimized, null, List.of());
        assertSame(retained, value.get(null));
    }
}
