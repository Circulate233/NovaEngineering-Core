package github.kasuminova.novaeng.mixin.ae2;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingAllocationTargetsTest {
    private static ClassNode dependency(final String name) throws Exception {
        try (final var input = CraftingAllocationTargetsTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            final ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    @Test
    void reservationKeepsTheOwnedMapAndDoesNotResizeNonemptyLists() throws Exception {
        final BulkMixin mixin = new BulkMixin();
        final var recordsField = MixinItemListBulkIteration.class.getDeclaredField("records");
        recordsField.setAccessible(true);
        final Reference2ObjectOpenHashMap<Object, Object> records = new Reference2ObjectOpenHashMap<>(2);
        recordsField.set(mixin, records);
        final var capacity = Reference2ObjectOpenHashMap.class.getDeclaredField("n");
        capacity.setAccessible(true);
        mixin.nova$reserveItemTypes(Integer.MAX_VALUE);
        assertSame(records, recordsField.get(mixin), "grow the owned map instead of replacing it");
        assertEquals(capacity.getInt(new Reference2ObjectOpenHashMap<>(4096)), capacity.getInt(records));
        records.put(new Object(), new Object());
        assertEquals(1, mixin.nova$itemTypeCount());
        final int before = capacity.getInt(records);
        mixin.nova$reserveItemTypes(0);
        assertEquals(before, capacity.getInt(records));

        final Reference2ObjectOpenHashMap<Object, Object> smallNonempty = new Reference2ObjectOpenHashMap<>(2);
        smallNonempty.put(new Object(), new Object());
        recordsField.set(mixin, smallNonempty);
        final int smallCapacity = capacity.getInt(smallNonempty);
        mixin.nova$reserveItemTypes(4096);
        assertEquals(smallCapacity, capacity.getInt(smallNonempty));
    }

    @Test
    void snapshotSourceIsLoadedOnceAfterItsOwnedCacheIsInitialized() throws Exception {
        final ClassNode inventory = dependency("appeng/crafting/MECraftingInventory");
        final var constructor = inventory.methods.stream()
                                                 .filter(method -> method.name.equals("<init>") && method.desc.equals("(Lappeng/api/storage/data/IItemList;)V"))
                                                 .findFirst().orElseThrow();
        boolean initialized = false;
        int sourceLoads = 0;
        for (final var instruction : constructor.instructions) {
            if (instruction instanceof FieldInsnNode field && field.name.equals("localCache") && field.getOpcode() == 181)
                initialized = true;
            if (instruction instanceof VarInsnNode variable && variable.getOpcode() == 25 && variable.var == 1) {
                assertTrue(initialized, "LOAD injection must never read an uninitialized localCache");
                sourceLoads++;
            }
        }
        assertEquals(1, sourceLoads, "extend the injection if the production constructor changes");
    }

    private static final class BulkMixin extends MixinItemListBulkIteration {
    }
}
