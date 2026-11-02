package github.kasuminova.novaeng.common.performance;

import WayofTime.bloodmagic.tile.TileAltar;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.tile.inventory.AppEngInternalInventory;
import github.kasuminova.novaeng.common.machine.MMAltar;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorController;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorPatternBus;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorWorker;
import github.kasuminova.novaeng.common.util.AutoCraftingQueue;
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager;
import github.kasuminova.novaeng.common.util.TileItemHandler;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import ink.ikx.mmce.common.utils.StructureIngredient;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaOptimizationTest {

    @BeforeAll
    static void bootstrap() {
        Bootstrap.register();
    }

    private static CapabilityDispatcher dispatcher(final int value) {
        final var provider = new ICapabilitySerializable<NBTTagCompound>() {
            @Override
            public boolean hasCapability(@Nonnull final Capability<?> capability,
                                         final EnumFacing facing) {
                return false;
            }

            @Override
            public <T> T getCapability(@Nonnull final Capability<T> capability,
                                       final EnumFacing facing) {
                return null;
            }

            @Override
            public NBTTagCompound serializeNBT() {
                final NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("value", value);
                return tag;
            }

            @Override
            public void deserializeNBT(final NBTTagCompound tag) {
            }
        };
        return new CapabilityDispatcher(Map.of(), provider);
    }

    static int linearFind(final List<ItemStack> stacks, final ItemStack stack, final boolean strict) {
        for (int i = 0; i < stacks.size(); i++) {
            if (ItemUtils.matchStacks(stack, stacks.get(i)) && (!strict || stack.getCount() == stacks.get(i).getCount())) {
                return i;
            }
        }
        return -1;
    }

    static NBTTagCompound originalWork(final EFabricatorWorker.CraftWork work, final List<ItemStack> stacks) {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("R#S", (byte) work.getRemaining().length);
        for (int i = 0; i < work.getRemaining().length; i++) {
            final ItemStack stack = work.getRemaining()[i];
            if (stack.isEmpty()) {
                continue;
            }
            int index = linearFind(stacks, stack, true);
            if (index < 0) {
                index = stacks.size();
                stacks.add(stack);
            }
            tag.setShort("R#" + i, (short) index);
        }
        final int output = linearFind(stacks, work.getOutput(), true);
        if (output >= 0) {
            tag.setShort("O", (short) output);
            return tag;
        }
        stacks.add(work.getOutput());
        tag.setShort("O", (short) (stacks.size() - 1));
        tag.setInteger("Z", work.getSize());
        return tag;
    }

    static void set(final Class<?> type, final Object object, final String name, final Object value) throws Exception {
        final Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static ICraftingPatternDetails pattern(final String name) {
        return (ICraftingPatternDetails) Proxy.newProxyInstance(JavaOptimizationTest.class.getClassLoader(),
            new Class<?>[]{ICraftingPatternDetails.class}, (proxy, method, args) -> switch (method.getName()) {
                case "isCraftable" -> true;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> name;
                default -> null;
            });
    }

    static Object objectMethod(final Object proxy, final Method method, final Object[] args) {
        return switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> proxy.getClass().getSimpleName();
            default -> null;
        };
    }

    @Test
    void drillTablesPreserveEverySignedByteIncludingNaNAndAllKeys() {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            final byte value = (byte) i;
            assertEquals(Double.doubleToLongBits(1000 * Math.pow(value, 1.6)),
                Double.doubleToLongBits(DrillOutputLookup.researchDamage(value)));
            assertEquals(Double.doubleToLongBits(1 + Math.pow(value, 2.5)),
                Double.doubleToLongBits(DrillOutputLookup.componentMultiplier(value)));
        }
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                assertEquals("parallelSize" + x + z, DrillOutputLookup.parallelKey(x, z));
                assertEquals("depletion" + x + z, DrillOutputLookup.depletionKey(x, z));
                assertEquals("veinName" + x + z, DrillOutputLookup.veinKey(x, z));
            }
        }
        final String[] names = {"litherite", "erodium", "kyronite", "pladium", "ionite", "aethium"};
        for (int i = 0; i < names.length; i++) {
            assertEquals("environmentaltech:" + names[i] + "_crystal", DrillOutputLookup.crystalName(i));
        }
        for (int level = 1; level <= 127; level++) {
            assertEquals(level == 1 ? 2 : level == 2 ? 4 : level == 3 ? 5 : 6,
                DrillOutputLookup.crystalCount(level));
        }
    }

    @Test
    void stackIndexPreservesFirstMatchAcrossThresholdCountsTagsAndRawDamage() {
        final Item unusual = new Item() {
            @Override
            public int getDamage(final ItemStack stack) {
                return stack.getCount();
            }
        };
        for (final boolean strict : new boolean[]{false, true}) {
            final List<ItemStack> linear = new ObjectArrayList<>();
            final List<ItemStack> indexed = new ObjectArrayList<>();
            final ItemStackIndex index = ItemStackIndex.open(indexed, strict);
            final Random random = new Random(78123);
            for (int i = 0; i < 2000; i++) {
                final ItemStack stack = new ItemStack(unusual, 1 + random.nextInt(4), random.nextInt(90));
                if (i % 3 != 0) {
                    final NBTTagCompound tag = new NBTTagCompound();
                    tag.setInteger("type", random.nextInt(5));
                    stack.setTagCompound(tag);
                }
                final int expected = linearFind(linear, stack, strict);
                assertEquals(expected, index.find(stack));
                final int added = expected < 0 ? linear.size() : expected;
                if (expected < 0) {
                    linear.add(stack);
                }
                assertEquals(added, index.findOrAdd(stack));
            }
            assertEquals(linear, indexed);
            final ItemStack copy = new ItemStack(unusual, 64, 301);
            indexed.add(copy);
            indexed.add(copy);
            assertEquals(indexed.size() - 2, index.find(copy));
            assertEquals(-1, index.find(ItemStack.EMPTY));
            indexed.add(ItemStack.EMPTY);
            assertEquals(indexed.size() - 1, index.find(ItemStack.EMPTY));
            index.close();
        }
    }

    @Test
    void craftWorkNbtMatchesOriginalWriterWithSharedTables() {
        final Item item = new Item();
        final List<ItemStack> expected = new ObjectArrayList<>();
        final List<ItemStack> actual = new ObjectArrayList<>();
        for (int i = 0; i < 120; i++) {
            final ItemStack input = new ItemStack(item, 2, i % 50);
            final ItemStack output = i % 4 == 0 ? input : new ItemStack(item, 3, i + 200);
            final var work = new EFabricatorWorker.CraftWork(new ItemStack[]{input, ItemStack.EMPTY}, output, 7);
            assertEquals(originalWork(work, expected), work.writeToNBT(actual));
            assertEquals(expected, actual);
        }
    }

    @Test
    void inventoryNbtKeepsOriginalFirstCountAndEmptyIndexes() {
        final Item item = new Item();
        final int[] slots = IntStream.range(0, 100).toArray();
        final TileItemHandler handler = new TileItemHandler(null, slots, slots, "test") {
            @Override
            public void setStackInSlot(final int slot, @Nonnull final ItemStack stack) {
                this.inventory[slot].itemStack.set(stack);
            }
        };
        final List<ItemStack> expected = new ObjectArrayList<>();
        final int[] indexes = new int[100];
        for (int i = 0; i < 100; i++) {
            final ItemStack stack = i % 11 == 0 ? ItemStack.EMPTY : new ItemStack(item, 1 + i, i % 45);
            handler.setStackInSlot(i, stack);
            indexes[i] = stack.isEmpty() ? -1 : linearFind(expected, stack, false);
            if (!stack.isEmpty() && indexes[i] < 0) {
                indexes[i] = expected.size();
                expected.add(stack);
            }
        }
        final NBTTagCompound tag = handler.writeNBT();
        final NBTTagList inv = tag.getTagList("invSet", 1);
        final NBTTagList stacks = tag.getTagList("stackSet", 10);
        assertEquals(expected.size(), stacks.tagCount());
        for (int i = 0; i < 100; i++) {
            assertEquals((byte) indexes[i], ((NBTTagByte) inv.get(i)).getByte());
        }
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).writeToNBT(new NBTTagCompound()), stacks.getCompoundTagAt(i));
        }
    }

    @Test
    void queuesReleaseOnlyTheRequestedPlayerOrWorldAndEmptyExecutionReleasesEntry() throws Exception {
        final Field mapField = AutoCraftingQueue.class.getDeclaredField("QUEUES");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked") final Map<EntityPlayer, AutoCraftingQueue> queues =
            (Map<EntityPlayer, AutoCraftingQueue>) mapField.get(null);
        AutoCraftingQueue.clearAll();
        final WorldServer firstWorld = WorldgenTestBlocks.allocate(WorldServer.class);
        final WorldServer secondWorld = WorldgenTestBlocks.allocate(WorldServer.class);
        final EntityPlayerMP first = WorldgenTestBlocks.allocate(EntityPlayerMP.class);
        final EntityPlayerMP second = WorldgenTestBlocks.allocate(EntityPlayerMP.class);
        first.world = firstWorld;
        second.world = secondWorld;
        final Field entityId = Entity.class.getDeclaredField("entityId");
        entityId.setAccessible(true);
        entityId.setInt(first, 1);
        entityId.setInt(second, 2);
        final AutoCraftingQueue firstQueue = new AutoCraftingQueue();
        final AutoCraftingQueue secondQueue = new AutoCraftingQueue();
        final ArrayDeque<ItemStack> firstItems = new ArrayDeque<>(List.of(ItemStack.EMPTY));
        final ArrayDeque<ItemStack> secondItems = new ArrayDeque<>(List.of(ItemStack.EMPTY));
        set(AutoCraftingQueue.class, firstQueue, "queue", firstItems);
        set(AutoCraftingQueue.class, secondQueue, "queue", secondItems);
        queues.put(first, firstQueue);
        queues.put(second, secondQueue);
        try {
            AutoCraftingQueue.removeWorld(firstWorld);
            assertNull(AutoCraftingQueue.getQueue(first));
            assertTrue(firstItems.isEmpty());
            assertSame(secondQueue, AutoCraftingQueue.getQueue(second));
            AutoCraftingQueue.removeQueue(second);
            assertTrue(secondItems.isEmpty());
            queues.put(first, firstQueue);
            assertFalse(firstQueue.executionQueue(first));
            assertTrue(queues.isEmpty());
            queues.put(second, secondQueue);
            secondQueue.clearQueue();
            assertTrue(queues.isEmpty());
        } finally {
            AutoCraftingQueue.clearAll();
        }
    }

    @Test
    void patternSnapshotStaysStableWhenRegistrationReentersAndCountTracksRefresh() throws Exception {
        final EFabricatorPatternBus bus = WorldgenTestBlocks.allocate(EFabricatorPatternBus.class);
        final ObjectArrayList<ICraftingPatternDetails> details = new ObjectArrayList<>();
        details.size(72);
        set(EFabricatorPatternBus.class, bus, "details", details);
        final AppEngInternalInventory inventory = new AppEngInternalInventory(null, 72);
        set(EFabricatorPatternBus.class, bus, "patterns", inventory);
        final ICraftingPatternDetails first = pattern("first");
        final ICraftingPatternDetails second = pattern("second");
        details.set(0, first);
        details.set(1, second);
        set(EFabricatorPatternBus.class, bus, "validPatterns", 2);
        final List<ICraftingPatternDetails> publicSnapshot = bus.getDetails();
        publicSnapshot.clear();
        assertEquals(2, bus.getValidPatterns());
        final Method refresh = EFabricatorPatternBus.class.getDeclaredMethod("refreshPattern", int.class);
        refresh.setAccessible(true);
        final Method provide = EFabricatorPatternBus.class.getDeclaredMethod("provideCrafting",
            ICraftingProvider.class, ICraftingProviderHelper.class);
        provide.setAccessible(true);
        final List<ICraftingPatternDetails> registered = new ObjectArrayList<>();
        final ICraftingProviderHelper tracker = (ICraftingProviderHelper) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{ICraftingProviderHelper.class}, (proxy, method, args) -> {
                if (method.getName().equals("addCraftingOption")) {
                    registered.add((ICraftingPatternDetails) args[1]);
                    if (registered.size() == 1) {
                        refresh.invoke(bus, 1);
                    }
                }
                return objectMethod(proxy, method, args);
            });
        provide.invoke(bus, null, tracker);
        assertEquals(List.of(first, second), registered);
        assertEquals(1, bus.getValidPatterns());
        assertEquals(List.of(first), bus.getDetails());
        registered.clear();
        provide.invoke(bus, null, tracker);
        assertEquals(List.of(first), registered);
        refresh.invoke(bus, 0);
        assertEquals(0, bus.getValidPatterns());
    }

    @Test
    @SuppressWarnings("unchecked")
    void workerAllowsSubUnitEnergyWithoutClampingExceptionOrConsumingWork() throws Exception {
        final var controller = WorldgenTestBlocks.allocate(
            EFabricatorController.class);
        controller.setParallelism(4);
        controller.setOutputBuffer((IItemList<IAEItemStack>)
            Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IItemList.class},
                JavaOptimizationTest::objectMethod));
        final EFabricatorWorker worker = WorldgenTestBlocks.allocate(EFabricatorWorker.class);
        set(EFabricatorWorker.class, worker, "queue", new EFabricatorWorker.CraftingQueue());
        worker.setController(controller);
        worker.offerWork(new EFabricatorWorker.CraftWork(new ItemStack[0], new ItemStack(Items.APPLE), 1));
        for (final int parallelism : new int[]{-1, 0, 4}) {
            controller.setParallelism(parallelism);
            for (final int energy : new int[]{0, 1, 50, 99}) {
                worker.setEnergyCache(energy);
                assertEquals(0, worker.doWork());
                assertEquals(energy, worker.getEnergyCache());
                assertEquals(1, worker.getQueue().size());
            }
        }
    }

    @Test
    void altarCacheUsesWeakValuesAndClearsOnlyTheUnloadedWorld() throws Exception {
        final Class<?> type = MMAltar.class;
        final Field field = type.getDeclaredField("CACHE");
        field.setAccessible(true);
        @SuppressWarnings("unchecked") final Map<TileMultiblockMachineController,
            WeakReference<TileAltar>> cache =
            (Map<TileMultiblockMachineController, WeakReference<TileAltar>>) field.get(null);
        final WorldServer first = WorldgenTestBlocks.allocate(WorldServer.class);
        final WorldServer second = WorldgenTestBlocks.allocate(WorldServer.class);
        final var firstController = WorldgenTestBlocks.allocate(
            EFabricatorController.class);
        final var secondController = WorldgenTestBlocks.allocate(
            EFabricatorController.class);
        final var firstAltar = WorldgenTestBlocks.allocate(TileAltar.class);
        final var secondAltar = WorldgenTestBlocks.allocate(TileAltar.class);
        firstController.setWorld(first);
        secondController.setWorld(second);
        firstAltar.setWorld(first);
        secondAltar.setWorld(second);
        cache.clear();
        try {
            cache.put(firstController, new WeakReference<>(firstAltar));
            cache.put(secondController, new WeakReference<>(secondAltar));
            assertSame(firstAltar, MMAltar.getAltar(firstController));
            MMAltar.removeWorld(first);
            assertFalse(cache.containsKey(firstController));
            assertSame(secondAltar, cache.get(secondController).get());
            cache.get(secondController).clear();
            MMAltar.removeWorld(first);
            assertTrue(cache.isEmpty());
            cache.put(secondController, new WeakReference<>(secondAltar));
            MMAltar.clearCache();
            assertTrue(cache.isEmpty());
        } finally {
            cache.clear();
        }
    }

    @Test
    void nestedAndExceptionalIndexesNeverShareOrRetainStackLists() throws Exception {
        final List<ItemStack> outerStacks = new ObjectArrayList<>();
        final List<ItemStack> innerStacks = new ObjectArrayList<>();
        final ItemStackIndex outer = ItemStackIndex.open(outerStacks, false);
        try (outer; final ItemStackIndex inner = ItemStackIndex.open(innerStacks, true)) {
            assertNotSame(outer, inner);
            for (int i = 0; i < 80; i++) {
                outer.findOrAdd(new ItemStack(new Item(), 1, i));
                inner.findOrAdd(new ItemStack(new Item(), 1, i));
            }
            assertEquals(80, outerStacks.size());
            assertEquals(80, innerStacks.size());
        }
        final Field source = ItemStackIndex.class.getDeclaredField("stacks");
        source.setAccessible(true);
        assertNull(source.get(outer));
        try (final ItemStackIndex reused = ItemStackIndex.open(new ObjectArrayList<>(), false)) {
            assertSame(outer, reused);
            assertEquals(-1, reused.find(new ItemStack(Items.APPLE)));
        }
    }

    @Test
    void materialAlternativesPreserveOrderFirstIdentityAndSingletonMerging() throws Exception {
        final Class<?> type = NEWMachineAssemblyManager.class;
        final Method items = type.getDeclaredMethod("getItemStackIngList", List.class);
        items.setAccessible(true);
        final ItemStack apple = new ItemStack(Items.APPLE, 3);
        final ItemStack duplicate = new ItemStack(Items.APPLE, 8);
        final ItemStack carrot = new ItemStack(Items.CARROT);
        final var position = BlockPos.ORIGIN;
        final var first = new StructureIngredient.ItemIngredient(position,
            List.of(new Tuple<>(apple, Blocks.STONE.getDefaultState()), new Tuple<>(duplicate, Blocks.STONE.getDefaultState()),
                new Tuple<>(carrot, Blocks.STONE.getDefaultState())), null);
        final var single = new StructureIngredient.ItemIngredient(position,
            List.of(new Tuple<>(duplicate, Blocks.STONE.getDefaultState())), null);
        @SuppressWarnings("unchecked") final List<List<ItemStack>> result = (List<List<ItemStack>>) items.invoke(null,
            List.of(first, single, single));
        assertEquals(2, result.size());
        assertEquals(2, result.getFirst().size());
        assertSame(apple, result.getFirst().getFirst());
        assertSame(carrot, result.getFirst().get(1));
        assertSame(duplicate, result.get(1).getFirst());
        assertEquals(9, duplicate.getCount());
        assertEquals(3, apple.getCount());
    }

    @Test
    void stackIndexKeepsForgeCapabilityComparisonAndNullTagException() throws Exception {
        final ItemStack first = new ItemStack(Items.APPLE);
        final ItemStack second = new ItemStack(Items.APPLE);
        final Field capabilityField = ItemStack.class.getDeclaredField("capabilities");
        capabilityField.setAccessible(true);
        capabilityField.set(first, dispatcher(1));
        capabilityField.set(second, dispatcher(2));
        final List<ItemStack> stacks = new ObjectArrayList<>();
        for (int i = 0; i < 40; i++) {
            stacks.add(new ItemStack(new Item()));
        }
        stacks.add(first);
        try (final ItemStackIndex index = ItemStackIndex.open(stacks, false)) {
            assertTrue(ItemUtils.matchStacks(second, first));
            assertEquals(40, index.find(second));
        }
        first.setTagCompound(new NBTTagCompound());
        second.setTagCompound(new NBTTagCompound());
        try (final ItemStackIndex index = ItemStackIndex.open(stacks, false)) {
            assertFalse(ItemUtils.matchStacks(second, first));
            assertEquals(-1, index.find(second));
            capabilityField.set(second, dispatcher(1));
            assertTrue(ItemUtils.matchStacks(second, first));
            assertEquals(40, index.find(second));
        }
    }
}
