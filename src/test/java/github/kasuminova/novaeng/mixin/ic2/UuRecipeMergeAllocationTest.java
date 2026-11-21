package github.kasuminova.novaeng.mixin.ic2;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UuRecipeMergeAllocationTest {
    private static StackSpec spec(final Random random) {
        return new StackSpec(random.nextInt(3), random.nextInt(4), random.nextInt(4) - 1, size(random));
    }

    private static int size(final Random random) {
        return switch (random.nextInt(12)) {
            case 0 -> Integer.MAX_VALUE;
            case 1 -> 0;
            default -> 1 + random.nextInt(64);
        };
    }

    @Test
    void installedMergeAndOptimizationAgreeOnAlternativeOrderCountsAndOriginalAliases() throws Exception {
        final Machine baseline = new Machine(false);
        final Machine optimized = new Machine(true);
        final Item[] items = {new Item().setHasSubtypes(true), new Item(), new Item().setHasSubtypes(true)};
        final Random random = new Random(0x554752415048L);
        for (int trial = 0; trial < 700; trial++) {
            final List<List<StackSpec>> inputs = new ArrayList<>();
            final int inputCount = random.nextInt(9);
            for (int i = 0; i < inputCount; i++) {
                final List<StackSpec> alternatives;
                if (!inputs.isEmpty() && random.nextBoolean()) {
                    alternatives = new ArrayList<>(inputs.get(random.nextInt(inputs.size())));
                    Collections.shuffle(alternatives, random);
                    alternatives.replaceAll(s -> new StackSpec(s.item, s.meta, s.tag, size(random)));
                } else {
                    alternatives = new ArrayList<>();
                    final int size = random.nextInt(7);
                    for (int j = 0; j < size; j++) alternatives.add(spec(random));
                }
                inputs.add(alternatives);
            }
            final List<StackSpec> outputs = new ArrayList<>();
            final int outputCount = random.nextInt(10);
            for (int i = 0; i < outputCount; i++) outputs.add(spec(random));
            assertEquals(baseline.merge(items, inputs, outputs), optimized.merge(items, inputs, outputs),
                "installed IC2 merge trial " + trial);
        }
    }

    @Test
    void lateMismatchesAndRepeatedAlternativesKeepFirstUnmatchedPairing() throws Exception {
        final Machine baseline = new Machine(false);
        final Machine optimized = new Machine(true);
        final Item[] items = {new Item(), new Item().setHasSubtypes(true)};
        final List<List<StackSpec>> inputs = List.of(
            List.of(new StackSpec(0, 1, 1, 3), new StackSpec(0, 2, 1, 5), new StackSpec(1, 1, 1, 7)),
            List.of(new StackSpec(0, 3, 1, 11), new StackSpec(0, 4, 1, 13), new StackSpec(1, 2, 1, 17)),
            List.of(new StackSpec(0, 4, 1, 19), new StackSpec(1, 1, 1, 23), new StackSpec(0, 5, 1, 29)),
            List.of(new StackSpec(0, 6, 1, Integer.MAX_VALUE), new StackSpec(0, 7, 1, 31), new StackSpec(1, 1, 1, 37))
        );
        final List<StackSpec> outputs = List.of(new StackSpec(0, 1, 0, 3), new StackSpec(0, 2, -1, 5));
        assertEquals(baseline.merge(items, inputs, outputs), optimized.merge(items, inputs, outputs));
    }

    @Test
    void differentSizedCandidateGroupsDoNotReusePriorPartialMatches() throws Exception {
        final Machine baseline = new Machine(false);
        final Machine optimized = new Machine(true);
        final Item[] items = {new Item().setHasSubtypes(true)};
        final List<List<StackSpec>> inputs = new ArrayList<>();
        for (final int length : new int[]{4, 2, 8, 4, 2, 8, 4, 2, 8}) {
            final List<StackSpec> group = new ArrayList<>();
            for (int i = 0; i < length; i++) group.add(new StackSpec(0, i, i, length + i));
            if (inputs.size() < 6) group.set(length - 1, new StackSpec(0, inputs.size() % 3 + 30, -1, 1));
            inputs.add(group);
        }
        assertEquals(baseline.merge(items, inputs, List.of()), optimized.merge(items, inputs, List.of()));
    }

    private record StackSpec(int item, int meta, int tag, int size) {
    }

    private record StackView(Item item, int meta, NBTTagCompound tag, int size, int originalAlias) {
    }

    private record GroupView(List<StackView> alternatives, int originalAlias) {
    }

    private record Result(List<GroupView> inputs, List<StackView> outputs) {
    }

    private static final class Machine {
        private final Constructor<?> stackConstructor;
        private final Constructor<?> recipeConstructor;
        private final Method merge;
        private final Method item;
        private final Method meta;
        private final Method tag;
        private final Method size;
        private final Field inputs;
        private final Field outputs;

        private Machine(final boolean optimized) throws Exception {
            final ClassLoader loader = new ClassLoader(UuRecipeMergeAllocationTest.class.getClassLoader()) {
                @Override
                protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                    if (!name.startsWith("ic2.core.uu.")) return super.loadClass(name, resolve);
                    Class<?> type = findLoadedClass(name);
                    if (type == null) {
                        try {
                            final var original = UuGraphAllocationTest.readClass(getParent(), name.replace('.', '/'));
                            if (optimized && name.equals("ic2.core.uu.RecipeTransformation")) {
                                final var mixin = UuGraphAllocationTest.readClass(getParent(),
                                    "github/kasuminova/novaeng/mixin/ic2/MixinRecipeTransformation");
                                int applied = 0;
                                for (final MethodNode method : mixin.methods) {
                                    if (!method.name.equals("merge") && !method.name.equals("mergeEqualLists")
                                        && !method.name.equals("nova$mergeEqualLists"))
                                        continue;
                                    method.visibleAnnotations = null;
                                    method.invisibleAnnotations = null;
                                    for (final var instruction : method.instructions) {
                                        if (instruction instanceof MethodInsnNode call && call.owner.equals(mixin.name))
                                            call.owner = original.name;
                                        if (instruction instanceof FieldInsnNode field && field.owner.equals(mixin.name))
                                            field.owner = original.name;
                                    }
                                    final boolean replaced = original.methods.removeIf(m -> m.name.equals(method.name) && m.desc.equals(method.desc));
                                    assertTrue(replaced || method.name.equals("nova$mergeEqualLists"));
                                    original.methods.add(method);
                                    applied++;
                                }
                                assertEquals(3, applied, "transplant both actual production overwrites and their call-local helper");
                            }
                            final ClassWriter out = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                            original.accept(out);
                            final byte[] bytes = out.toByteArray();
                            type = defineClass(name, bytes, 0, bytes.length);
                        } catch (Exception failure) {
                            throw new ClassNotFoundException(name, failure);
                        }
                    }
                    if (resolve) resolveClass(type);
                    return type;
                }
            };
            final Class<?> stack = loader.loadClass("ic2.core.uu.LeanItemStack");
            final Class<?> recipe = loader.loadClass("ic2.core.uu.RecipeTransformation");
            stackConstructor = stack.getDeclaredConstructor(Item.class, int.class, NBTTagCompound.class, int.class);
            stackConstructor.setAccessible(true);
            recipeConstructor = recipe.getDeclaredConstructor(double.class, List.class, List.class);
            merge = recipe.getDeclaredMethod("merge");
            merge.setAccessible(true);
            item = stack.getMethod("getItem");
            meta = stack.getMethod("getMeta");
            tag = stack.getMethod("getNbt");
            size = stack.getMethod("getSize");
            inputs = recipe.getField("inputs");
            outputs = recipe.getField("outputs");
        }

        private Result merge(final Item[] items, final List<List<StackSpec>> inputSpecs,
                             final List<StackSpec> outputSpecs) throws Exception {
            final Map<Object, Integer> originalStacks = new IdentityHashMap<>();
            final Map<Object, Integer> originalGroups = new IdentityHashMap<>();
            final List<List<Object>> sourceInputs = new ArrayList<>();
            for (final List<StackSpec> specs : inputSpecs) {
                final List<Object> alternatives = new ArrayList<>();
                for (final StackSpec spec : specs) alternatives.add(create(items, spec, originalStacks));
                originalGroups.put(alternatives, originalGroups.size());
                sourceInputs.add(alternatives);
            }
            final List<Object> sourceOutputs = new ArrayList<>();
            for (final StackSpec spec : outputSpecs) sourceOutputs.add(create(items, spec, originalStacks));
            final List<GroupView> sourceInputSnapshot = snapshotGroups(sourceInputs, originalGroups, originalStacks);
            final List<StackView> sourceOutputSnapshot = snapshotStacks(sourceOutputs, originalStacks);
            final Object transformation = recipeConstructor.newInstance(2.5, sourceInputs, sourceOutputs);
            merge.invoke(transformation);
            assertEquals(sourceInputSnapshot, snapshotGroups(sourceInputs, originalGroups, originalStacks),
                "merging must not mutate resolver-owned input lists or stacks");
            assertEquals(sourceOutputSnapshot, snapshotStacks(sourceOutputs, originalStacks),
                "merging must not mutate resolver-owned output lists or stacks");
            @SuppressWarnings("unchecked") final List<List<Object>> mergedInputs = (List<List<Object>>) inputs.get(transformation);
            @SuppressWarnings("unchecked") final List<Object> mergedOutputs = (List<Object>) outputs.get(transformation);
            return new Result(snapshotGroups(mergedInputs, originalGroups, originalStacks),
                snapshotStacks(mergedOutputs, originalStacks));
        }

        private Object create(final Item[] items, final StackSpec spec, final Map<Object, Integer> aliases) throws Exception {
            final NBTTagCompound nbt;
            if (spec.tag < 0) {
                nbt = null;
            } else {
                nbt = new NBTTagCompound();
                if (spec.tag != 0) nbt.setInteger("variant", spec.tag);
            }
            final Object stack = stackConstructor.newInstance(items[spec.item], spec.meta, nbt, spec.size);
            aliases.put(stack, aliases.size());
            return stack;
        }

        private List<GroupView> snapshotGroups(final List<List<Object>> groups, final Map<Object, Integer> groupAliases,
                                               final Map<Object, Integer> stackAliases) throws Exception {
            final List<GroupView> snapshot = new ArrayList<>();
            for (final List<Object> group : groups)
                snapshot.add(new GroupView(snapshotStacks(group, stackAliases), groupAliases.getOrDefault(group, -1)));
            return snapshot;
        }

        private List<StackView> snapshotStacks(final List<Object> stacks, final Map<Object, Integer> aliases) throws Exception {
            final List<StackView> snapshot = new ArrayList<>();
            for (final Object stack : stacks)
                snapshot.add(new StackView((Item) item.invoke(stack),
                    (int) meta.invoke(stack), (NBTTagCompound) tag.invoke(stack), (int) size.invoke(stack), aliases.getOrDefault(stack, -1)));
            return snapshot;
        }
    }
}
