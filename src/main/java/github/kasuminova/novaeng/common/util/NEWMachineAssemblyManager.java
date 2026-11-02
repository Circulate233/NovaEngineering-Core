package github.kasuminova.novaeng.common.util;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.fluids.util.AEFluidStack;
import appeng.me.helpers.PlayerSource;
import appeng.util.item.AEItemStack;
import com.circulation.random_complement.common.util.MEHandler;
import com.glodblock.github.common.item.fake.FakeFluids;
import github.kasuminova.novaeng.NovaEngineeringCore;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.ModIntegrationJEI;
import hellfirepvp.modularmachinery.common.integration.preview.StructurePreviewWrapper;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.network.PktAssemblyReport;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import ink.ikx.mmce.common.assembly.MachineAssembly;
import ink.ikx.mmce.common.utils.StructureIngredient;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class NEWMachineAssemblyManager {

    public static Collection<DynamicMachine> getAllDynamicMachines() {
        final List<DynamicMachine> result = new ObjectArrayList<>();
        for (final var map : State.CONSTRUCTORS.values()) {
            result.addAll(map.values());
        }
        return result;
    }

    @Nullable
    public static DynamicMachine getDynamicMachine(final IBlockState state) {
        return getDynamicMachine(state.getBlock(), state.getBlock().getMetaFromState(state));
    }

    @Nullable
    public static DynamicMachine getDynamicMachine(final Block block, final int meta) {
        final var map = State.CONSTRUCTORS.get(block);
        return map == null ? null : map.get(meta);
    }

    public static ObjectIterator<Map.Entry<Block, Int2ObjectOpenHashMap<DynamicMachine>>> getConstructorsIterator() {
        return State.CONSTRUCTORS.entrySet().iterator();
    }

    public static void setConstructors(final BlockPair block, final NEWDynamicMachine machine) {
        State.CONSTRUCTORS.computeIfAbsent(block.block(), ignored -> new Int2ObjectOpenHashMap<>()).put(block.meta(), machine);
        if (NovaEngineeringCore.proxy.isClient()) {
            ModIntegrationJEI.PREVIEW_WRAPPERS.add(new StructurePreviewWrapper(machine));
        }
    }

    public static AssemblyBlockArray addAssemblyMachine(final EntityPlayer player, final BlockArray array) {
        final var assembly = new AssemblyBlockArray(array);
        State.ASSEMBLIES.put(player, assembly);
        return assembly;
    }

    @Nullable
    public static AssemblyBlockArray getMachineAssembly(final EntityPlayer player) {
        return State.ASSEMBLIES.get(player);
    }

    public static boolean checkMachineAssembly(final EntityPlayer player) {
        return State.ASSEMBLIES.containsKey(player);
    }

    public static void removeMachineAssembly(final EntityPlayer player) {
        final var assembly = State.ASSEMBLIES.remove(player);
        if (assembly != null) {
            assembly.end();
        }
    }

    public static Miss2ListPair checkAllItems(final EntityPlayer player, final StructureIngredient ingredient,
                                              final boolean usingAE, final boolean autoAECrafting) {
        if (player.isCreative()) {
            return State.EMPTY;
        }
        final List<ItemStack> inventory = new ObjectArrayList<>(player.inventory.mainInventory.size());
        for (final var stack : player.inventory.mainInventory) {
            inventory.add(stack.copy());
        }
        final var itemIngredients = ingredient.itemIngredient();
        final var fluidIngredients = ingredient.fluidIngredient();
        MachineAssembly.searchAndRemoveContainItem(inventory, itemIngredients);
        AssemblyBlockArray.searchAndRemoveContainFluid(inventory, fluidIngredients);
        if (itemIngredients.isEmpty() && fluidIngredients.isEmpty()) {
            return State.EMPTY;
        }
        final var items = getItemStackIngList(itemIngredients);
        final var fluids = getFluidStackIngList(fluidIngredients);
        if (usingAE) {
            final var terminal = MEHandler.getTerminalGuiObject(player);
            if (terminal != null && terminal.getActionableNode() != null) {
                final var grid = terminal.getActionableNode().getGrid();
                if (grid != null) {
                    final IStorageGrid storage = grid.getCache(IStorageGrid.class);
                    final var itemInventory = storage.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
                    final var fluidInventory = storage.getInventory(AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class));
                    final var source = new PlayerSource(player, terminal);
                    for (final var stacks : new ReferenceArrayList<>(items)) {
                        for (final var stack : stacks) {
                            final var extracted = itemInventory.extractItems(AEItemStack.fromItemStack(stack), Actionable.SIMULATE, source);
                            if (extracted == null) {
                                continue;
                            }
                            final int size = (int) extracted.getStackSize();
                            if (stack.getCount() <= size) {
                                items.remove(stacks);
                                break;
                            }
                            for (final var alternative : stacks) {
                                alternative.shrink(size);
                            }
                        }
                    }
                    for (final var stacks : new ReferenceArrayList<>(fluids)) {
                        for (final var stack : stacks) {
                            final var extracted = fluidInventory.extractItems(AEFluidStack.fromFluidStack(stack), Actionable.SIMULATE, source);
                            if (extracted == null) {
                                continue;
                            }
                            final int size = (int) extracted.getStackSize();
                            if (stack.amount <= size) {
                                fluids.remove(stacks);
                                break;
                            }
                            for (final var alternative : stacks) {
                                alternative.amount -= size;
                            }
                        }
                    }
                }
            }
            if (items.isEmpty() && fluids.isEmpty()) {
                return State.EMPTY;
            }
        }
        int missing = 0;
        for (final var stacks : items) {
            if (!stacks.isEmpty()) {
                missing += stacks.getFirst().getCount();
            }
        }
        for (final var stacks : fluids) {
            if (!stacks.isEmpty()) {
                missing += (stacks.getFirst().amount + 999) / 1000;
            }
        }
        final var packet = new PktAssemblyReport(items, fluids);
        if (player instanceof EntityPlayerMP serverPlayer) {
            ModularMachinery.NET_CHANNEL.sendTo(packet, serverPlayer);
        }
        if (!autoAECrafting) {
            return new Miss2ListPair(missing, State.COMPLETE);
        }
        final List<List<ItemStack>> result = new ObjectArrayList<>(items);
        for (final var stacks : fluids) {
            final List<ItemStack> packed = new ObjectArrayList<>();
            for (final var stack : stacks) {
                final var drops = FakeFluids.packFluid2Drops(stack);
                drops.setCount(stack.amount);
                packed.add(drops);
            }
            result.add(packed);
        }
        return new Miss2ListPair(missing, result);
    }

    private static List<List<FluidStack>> getFluidStackIngList(final List<StructureIngredient.FluidIngredient> ingredients) {
        final List<List<FluidStack>> result = new ObjectArrayList<>();
        ingredientLoop:
        for (final var ingredient : ingredients) {
            if (ingredient.ingredientList().isEmpty()) {
                continue;
            }
            final List<FluidStack> alternatives = new ObjectArrayList<>(ingredient.ingredientList().size());
            for (final var pair : ingredient.ingredientList()) {
                alternatives.add(pair.getFirst());
            }
            if (alternatives.size() == 1) {
                final var stack = alternatives.getFirst();
                for (final var existing : result) {
                    if (existing.size() == 1 && stack.isFluidEqual(existing.getFirst())) {
                        existing.getFirst().amount += 1000;
                        continue ingredientLoop;
                    }
                }
            }
            result.add(alternatives);
        }
        return result;
    }

    private static List<List<ItemStack>> getItemStackIngList(final List<StructureIngredient.ItemIngredient> ingredients) {
        final List<List<ItemStack>> result = new ObjectArrayList<>();
        ingredientLoop:
        for (final var ingredient : ingredients) {
            if (ingredient.ingredientList().isEmpty()) {
                continue;
            }
            final var alternatives = ingredient.ingredientList();
            if (alternatives.size() == 1) {
                final var stack = alternatives.getFirst().getFirst();
                for (final var existing : result) {
                    if (existing.size() == 1 && ItemUtils.matchStacks(stack, existing.getFirst())) {
                        existing.getFirst().grow(1);
                        continue ingredientLoop;
                    }
                }
            }
            final List<ItemStack> filtered = new ObjectArrayList<>(alternatives.size());
            stackLoop:
            for (final var pair : alternatives) {
                final ItemStack stack = pair.getFirst();
                for (final var existing : filtered) {
                    if (ItemUtils.matchStacks(stack, existing)) {
                        continue stackLoop;
                    }
                }
                filtered.add(stack);
            }
            result.add(filtered);
        }
        return result;
    }

    public enum OperatingStatus {SUCCESS, ALREADY_EXISTS, FAILURE, COMPLETE}

    private static final class State {
        private static final Reference2ObjectOpenHashMap<Block, Int2ObjectOpenHashMap<DynamicMachine>> CONSTRUCTORS = new Reference2ObjectOpenHashMap<>();
        private static final Map<EntityPlayer, AssemblyBlockArray> ASSEMBLIES = new Object2ObjectOpenHashMap<>();
        private static final List<List<ItemStack>> COMPLETE = ObjectLists.singleton(ObjectLists.singleton(ItemStack.EMPTY));
        private static final Miss2ListPair EMPTY = new Miss2ListPair(0, ObjectLists.emptyList());
    }

    public record BlockPair(Block block, int meta) {
        public Block getBlock() {
            return this.block;
        }

        public int getMeta() {
            return this.meta;
        }

        @SuppressWarnings("deprecation")
        public IBlockState getBlockState() {
            return this.block.getStateFromMeta(this.meta);
        }
    }

    public record Miss2ListPair(int miss, List<List<ItemStack>> list) {
        public int getMiss() {
            return this.miss;
        }

        public List<List<ItemStack>> getList() {
            return this.list;
        }
    }

    public static final class Ingredient {
        private final Object ingredient;
        private final boolean item;
        private final IAEStack<?> aeStack;

        public Ingredient(final ItemStack stack) {
            this.ingredient = stack;
            this.item = true;
            this.aeStack = AEItemStack.fromItemStack(stack);
        }

        public Ingredient(final FluidStack stack) {
            this.ingredient = stack;
            this.item = false;
            this.aeStack = AEFluidStack.fromFluidStack(stack);
        }

        public Object getIngredient() {
            return this.ingredient;
        }

        public boolean isItem() {
            return this.item;
        }

        public IAEStack<?> getAeStack() {
            return this.aeStack;
        }

        public ItemStack getItemStack() {
            return (ItemStack) this.ingredient;
        }

        public IAEItemStack getAEItemStack() {
            return this.aeStack instanceof IAEItemStack stack ? stack : null;
        }

        public FluidStack getFluidStack() {
            return (FluidStack) this.ingredient;
        }

        public IAEFluidStack getAEFluidStack() {
            return this.aeStack instanceof IAEFluidStack stack ? stack : null;
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Ingredient that && this.item == that.item && Objects.equals(this.aeStack, that.aeStack);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.aeStack);
        }
    }
}
