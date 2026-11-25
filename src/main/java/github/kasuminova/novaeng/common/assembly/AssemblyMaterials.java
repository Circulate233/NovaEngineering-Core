package github.kasuminova.novaeng.common.assembly;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
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
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.glodblock.github.common.item.ItemFluidPacket;
import github.kasuminova.novaeng.common.util.AutoCraftingQueue;
import hellfirepvp.modularmachinery.common.util.BlockArray.BlockInformation;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import ink.ikx.mmce.common.utils.FluidUtils;
import ink.ikx.mmce.common.utils.StackUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

/**
 * A construction's material plan and one reusable placement transaction.
 */
final class AssemblyMaterials {
    private final EntityPlayerMP player;
    private final InventoryPlayer inventory;
    private final AssemblyInventory hotbarAndStorage;
    private final boolean usingAE;
    private final boolean autoCraft;
    private final boolean creative;
    private final Reference2ObjectOpenHashMap<BlockInformation, Recipe> recipes = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<IBlockState, Option> options = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<Item, ItemKey> indexedItems = new Reference2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<IAEFluidStack, FluidMaterial> fluidKeys = new Object2ObjectOpenHashMap<>();
    private final FluidPlan fluids = new FluidPlan();
    private int scan;
    private boolean meResolved;
    private IGrid grid;
    private PlayerSource source;
    private IMEMonitor<IAEItemStack> meItems;
    private IMEMonitor<IAEFluidStack> meFluids;
    private Preflight preflight;
    private MissingReport report;
    private IBlockState reservedState;
    private int itemSlot = -1;
    private ItemStack itemDebited = ItemStack.EMPTY;
    private IAEItemStack meItemDebited;
    private IAEFluidStack meFluidDebited;
    private boolean fluidPlanReady;
    private int fluidSlotsWritten;
    private AssemblyDrops drops;

    AssemblyMaterials(final EntityPlayerMP player, final boolean usingAE, final boolean autoCraft) {
        this.player = player;
        inventory = player.inventory;
        hotbarAndStorage = new AssemblyInventory(player);
        creative = player.isCreative();
        this.usingAE = usingAE;
        this.autoCraft = autoCraft && this.usingAE;
    }

    Recipe recipe(final BlockInformation info) {
        var cached = recipes.get(info);
        if (cached != null) return cached;
        IBlockState sample = null;
        boolean acceptsAir = false;
        boolean fluidOnly = true;
        boolean firstFluid = false;
        boolean staticOnly = true;
        final var matchingTag = info.getMatchingTag();
        final boolean checkTile = info.getNBTChecker() != null || matchingTag != null && !matchingTag.isEmpty();
        LongArraySet states = null;
        long firstState = -1L;
        final var alternatives = creative ? null : new ObjectArrayList<Option>();
        for (final var descriptor : info.getMatchingStates()) {
            for (final var state : descriptor.getApplicable()) {
                if (state.getBlock() == Blocks.AIR) acceptsAir = true;
                if (!checkTile) {
                    final long id = stateKey(state);
                    if (firstState == -1) firstState = id;
                    else if (id != firstState) {
                        if (states == null) states = new LongArraySet();
                        states.add(id);
                    }
                }
                if (sample == null) {
                    sample = state;
                    firstFluid = AssemblyPlacement.fluid(state);
                }
                if (alternatives == null) {
                    fluidOnly &= AssemblyPlacement.fluid(state);
                    continue;
                }
                var option = options.get(state);
                if (option == null) {
                    option = option(state);
                    options.put(state, option);
                }
                fluidOnly &= option.fluid != null;
                if (option.item == null && option.fluid == null) continue;
                staticOnly &= AssemblyPlacement.isStatic(state);
                boolean duplicate = false;
                for (int i = 0; i < alternatives.size(); i++) {
                    final var old = alternatives.get(i);
                    if (old.item == option.item && old.fluid == option.fluid) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) alternatives.add(option);
            }
        }
        cached = new Recipe(checkTile ? info : null, sample, acceptsAir, sample != null && (creative ? firstFluid : fluidOnly),
            creative ? sample != null && AssemblyPlacement.isStatic(sample) : staticOnly,
            firstState, states, alternatives == null || alternatives.isEmpty() ? Recipe.NO_OPTIONS : alternatives.toArray(Option[]::new));
        recipes.put(info, cached);
        return cached;
    }

    private Option option(final IBlockState state) {
        final var liquid = state.getBlock() instanceof IFluidBlock block ? block.getFluid() : null;
        final FluidStack fluid = liquid == null ? FluidUtils.getFluidStackFromBlockState(state) : new FluidStack(liquid, 1000);
        if (fluid != null) {
            // The same fluid may occur under several states or descriptors.
            final var key = AEFluidStack.fromFluidStack(fluid);
            var material = fluidKeys.computeIfAbsent(key, ignored -> new FluidMaterial(fluid, key));
            return new Option(state, null, material);
        }
        if (state.getBlock() == Blocks.AIR) return new Option(state, null, null);
        final var stack = StackUtils.getStackFromBlockState(state);
        if (stack.isEmpty()) return new Option(state, null, null);
        var material = indexedItems.get(stack.getItem());
        while (material != null && !ItemUtils.matchStacks(stack, material.stack)) material = material.next;
        if (material == null) {
            material = new ItemKey(stack);
            material.ae = usingAE ? AEItemStack.fromItemStack(stack) : null;
            material.next = indexedItems.put(stack.getItem(), material);
        }
        return new Option(state, material, null);
    }

    private static long stateKey(final IBlockState state) {
        final Block block = state.getBlock();
        return ((long) Block.getIdFromBlock(block) << Integer.SIZE)
            | (block.getMetaFromState(state) & 0xFFFFFFFFL);
    }

    void beginPreflight() {
        if (!creative) preflight = new Preflight();
    }

    boolean reserve(final Recipe recipe) {
        reservedState = null;
        clearFluidPlan();
        if (player.isCreative() || recipe.sample != null && recipe.sample.getBlock() == Blocks.AIR) {
            reservedState = recipe.sample;
            return reservedState != null;
        }
        final var preview = preflight == null ? (preflight = new Preflight()) : preflight;
        // Items and fluid containers in the player's inventory have priority over wireless stock.
        for (final var option : recipe.options) {
            if (option.item != null ? preview.takeItem(option.item) : preview.takeFluid(option.fluid, false) == 0) {
                reservedState = option.state;
                return true;
            }
        }
        if (usingAE) {
            for (final var option : recipe.options) {
                if (option.item != null ? preview.takeFromME(option.item.ae, 1) : preview.takeFluid(option.fluid, true) == 0) {
                    reservedState = option.state;
                    return true;
                }
            }
        }
        preview.missing(recipe);
        return false;
    }

    @Nullable
    IBlockState reservedState() {
        return reservedState;
    }

    /**
     * Advance one material report or crafting request within the construction budget.
     */
    boolean finishPreflight() {
        if (report == null) {
            final var preview = preflight;
            preflight = null;
            clearFluidPlan();
            if (preview == null || preview.missing == null) return true;
            report = preview.missing;
        }
        if (!report.step()) return false;
        report = null;
        return true;
    }

    void refresh() {
        scan++;
        hotbarAndStorage.refresh();
        meResolved = false;
        grid = null;
        source = null;
        meItems = null;
        meFluids = null;
        if (preflight != null) preflight.availableME.clear();
        if (report != null) {
            report.preview.availableME.clear();
            report.refreshQuotes();
        }
    }

    private void resolveME() {
        if (meResolved) return;
        meResolved = true;
        if (!usingAE) return;
        final var terminal = MEHandler.getTerminalGuiObject(player);
        final var node = terminal == null ? null : terminal.getActionableNode();
        grid = node == null ? null : node.getGrid();
        if (grid == null) return;
        final IStorageGrid storage = grid.getCache(IStorageGrid.class);
        final var channels = AEApi.instance().storage();
        meItems = storage.getInventory(channels.getStorageChannel(IItemStorageChannel.class));
        meFluids = storage.getInventory(channels.getStorageChannel(IFluidStorageChannel.class));
        source = new PlayerSource(player, terminal);
    }

    private long simulate(final IAEStack<?> key, final long amount) {
        resolveME();
        return key instanceof IAEItemStack item ? available(meItems, item, amount) : available(meFluids, (IAEFluidStack) key, amount);
    }

    private <T extends IAEStack<T>> long available(@Nullable final IMEMonitor<T> monitor, final T key, final long amount) {
        if (monitor == null) return 0;
        final var accessible = monitor.extractItems(key.copy().setStackSize(amount), Actionable.SIMULATE, source);
        return accessible == null || !key.equals(accessible) ? 0 : Math.clamp(accessible.getStackSize(), 0L, amount);
    }

    @Nullable
    IBlockState take(final Recipe recipe, @Nullable final IBlockState preferred) {
        if (player.isCreative() || recipe.sample != null && recipe.sample.getBlock() == Blocks.AIR)
            return recipe.sample;
        Option preferredOption = null;
        if (preferred != null) {
            for (final var option : recipe.options) {
                if (option.state == preferred) {
                    preferredOption = option;
                    if (takeLocal(option)) return preferred;
                    break;
                }
            }
        }
        // A preflight reservation fixes the material class for this placement. Do not
        // consume a fluid for a solid reservation (or the reverse) when an alternative
        // state is available in the same BlockInformation.
        final boolean fluidStage = preferredOption != null && AssemblyPlacement.fluid(preferredOption.state);
        for (final var option : recipe.options) {
            if (option != preferredOption && (preferredOption == null || AssemblyPlacement.fluid(option.state) == fluidStage)
                && takeLocal(option)) return option.state;
        }
        if (usingAE) {
            if (preferredOption != null && takeME(preferredOption)) return preferred;
            for (final var option : recipe.options) {
                if (option != preferredOption && (preferredOption == null || AssemblyPlacement.fluid(option.state) == fluidStage)
                    && takeME(option)) return option.state;
            }
        }
        clearFluidPlan();
        return null;
    }

    private boolean takeLocal(final Option option) {
        if (option.item != null) {
            final var item = option.item;
            final int slot = item.slot(hotbarAndStorage, scan);
            if (slot < 0) return false;
            clearFluidPlan();
            itemSlot = slot;
            itemDebited = hotbarAndStorage.extract(slot, 1, false);
            if (itemDebited.getCount() != 1 || !ItemUtils.matchStacks(item.stack, itemDebited)) {
                refund();
                return false;
            }
            inventory.markDirty();
            return true;
        }
        if (collectFluid(hotbarAndStorage, option.fluid.stack) != 0) return false;
        writeFluidSlots();
        return true;
    }

    private boolean takeME(final Option option) {
        resolveME();
        if (option.item != null) {
            if (meItems == null) return false;
            clearFluidPlan();
            meItemDebited = meItems.extractItems(option.item.ae, Actionable.MODULATE, source);
            if (meItemDebited != null && meItemDebited.getStackSize() == 1 && option.item.ae.equals(meItemDebited))
                return true;
            refund();
            return false;
        }
        if (meFluids == null) return false;
        final var fluid = option.fluid;
        final int remaining = collectFluid(hotbarAndStorage, fluid.stack);
        if (remaining != 0) {
            final var request = remaining == fluid.stack.amount ? fluid.ae : fluid.ae.copy().setStackSize(remaining);
            var found = meFluids.extractItems(request, Actionable.SIMULATE, source);
            if (found == null || found.getStackSize() < remaining) {
                return false;
            }
            meFluidDebited = meFluids.extractItems(request, Actionable.MODULATE, source);
            if (meFluidDebited == null || meFluidDebited.getStackSize() != remaining || !fluid.ae.equals(meFluidDebited)) {
                refund();
                return false;
            }
        }
        for (int i = 0; i < fluids.edits; i++) {
            if (hotbarAndStorage.get(fluids.slots[i]) != fluids.before[i]) {
                refund();
                return false;
            }
        }
        writeFluidSlots();
        return true;
    }

    private void writeFluidSlots() {
        while (fluidSlotsWritten < fluids.edits) {
            final int i = fluidSlotsWritten;
            fluids.written[i] = fluids.after[i].copy();
            hotbarAndStorage.set(fluids.slots[i], fluids.after[i]);
            fluidSlotsWritten++;
        }
        if (fluidSlotsWritten != 0) inventory.markDirty();
    }

    void commit() {
        try {
            for (int i = 0; i < fluids.returned.size(); i++) give(fluids.returned.get(i));
        } finally {
            resetPayment();
        }
    }

    void refund() {
        try {
            refundLocal();
        } finally {
            try {
                refundME();
            } finally {
                resetPayment();
            }
        }
    }

    private void refundLocal() {
        if (!itemDebited.isEmpty()) {
            give(hotbarAndStorage.insert(itemSlot, itemDebited));
            inventory.markDirty();
        }
        for (int i = fluidSlotsWritten - 1; i >= 0; i--) {
            final int slot = fluids.slots[i];
            final var current = hotbarAndStorage.get(slot);
            if (current == fluids.after[i] && ItemStack.areItemStacksEqual(current, fluids.written[i])) {
                hotbarAndStorage.set(slot, fluids.before[i]);
            } else {
                // An event changed the slot after payment. Return the liquid without replacing its new contents.
                give(FakeFluids.packFluid2Packet(new FluidStack(fluids.drained, fluids.amounts[i])));
                for (int j = fluids.returnStarts[i], end = fluids.returnStarts[i + 1]; j < end; j++)
                    give(fluids.returned.get(j));
            }
        }
        if (fluidSlotsWritten != 0) inventory.markDirty();
    }

    private void refundME() {
        try {
            if (meItemDebited != null && meItemDebited.getStackSize() > 0) {
                final var left = meItems.injectItems(meItemDebited, Actionable.MODULATE, source);
                if (left != null) give(left.createItemStack());
            }
        } finally {
            if (meFluidDebited != null && meFluidDebited.getStackSize() > 0) {
                final var left = meFluids.injectItems(meFluidDebited, Actionable.MODULATE, source);
                if (left != null) give(FakeFluids.packFluid2Packet(left.getFluidStack()));
            }
        }
    }

    private void give(final ItemStack stack) {
        if (!stack.isEmpty()) inventory.placeItemBackInInventory(player.world, stack);
    }

    AssemblyDrops captureDrops() {
        if (drops == null) drops = new AssemblyDrops(player, this);
        return drops.begin();
    }

    void retain(final ItemStack stack) {
        if (!stack.isEmpty()) AssemblyRecovery.keep(player.getServerWorld(), player.getUniqueID(), stack);
    }

    static ItemStack recoverInventory(final EntityPlayerMP player, final ItemStack stack) {
        return stack.isEmpty() ? stack : ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.inventory), stack, false);
    }

    ItemStack recover(final ItemStack stack) {
        if (stack.isEmpty()) return stack;
        resolveME();
        if (ItemFluidPacket.isFluidPacket(stack)) {
            final FluidStack fluid = FakeItemRegister.getStack(stack);
            if (fluid != null && meFluids != null) {
                final var left = meFluids.injectItems(AEFluidStack.fromFluidStack(fluid), Actionable.MODULATE, source);
                if (left == null) return ItemStack.EMPTY;
                return recoverInventory(player, FakeFluids.packFluid2Packet(left.getFluidStack()));
            }
        } else if (meItems != null) {
            final var left = meItems.injectItems(AEItemStack.fromItemStack(stack), Actionable.MODULATE, source);
            return left == null ? ItemStack.EMPTY : recoverInventory(player, left.createItemStack());
        }
        return recoverInventory(player, stack);
    }

    void recoverFluid(final FluidStack fluid) {
        resolveME();
        int remaining = fluid.amount;
        if (meFluids != null) {
            final var left = meFluids.injectItems(AEFluidStack.fromFluidStack(fluid), Actionable.MODULATE, source);
            remaining = left == null ? 0 : (int) left.getStackSize();
        }
        final var inventory = new PlayerInvWrapper(player.inventory);
        for (int slot = 0; slot < inventory.getSlots() && remaining > 0; slot++) {
            remaining = fillRecovered(inventory, slot, fluid, remaining);
            final var stack = inventory.getStackInSlot(slot);
            final var nested = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (nested instanceof IItemHandlerModifiable writable)
                for (int inner = 0; inner < writable.getSlots() && remaining > 0; inner++)
                    remaining = fillRecovered(writable, inner, fluid, remaining);
        }
        if (remaining > 0)
            retain(recoverInventory(player, FakeFluids.packFluid2Packet(new FluidStack(fluid, remaining))));
        player.inventory.markDirty();
    }

    private int fillRecovered(final IItemHandlerModifiable inventory, final int slot, final FluidStack fluid, int remaining) {
        final int containers = inventory.getStackInSlot(slot).getCount();
        for (int attempt = 0; attempt < containers && remaining > 0; attempt++) {
            final var original = inventory.getStackInSlot(slot);
            final var copy = original.copy();
            copy.setCount(1);
            final var handler = FluidUtil.getFluidHandler(copy);
            if (handler == null) break;
            final int accepted = handler.fill(new FluidStack(fluid, remaining), true);
            if (accepted <= 0) break;
            if (original.getCount() == 1) inventory.setStackInSlot(slot, handler.getContainer());
            else {
                if (inventory.extractItem(slot, 1, false).isEmpty()) break;
                retain(recoverInventory(player, handler.getContainer()));
            }
            remaining -= accepted;
        }
        return remaining;
    }

    private void resetPayment() {
        itemSlot = -1;
        itemDebited = ItemStack.EMPTY;
        meItemDebited = null;
        meFluidDebited = null;
        fluidSlotsWritten = 0;
        clearFluidPlan();
    }

    private int collectFluid(final List<ItemStack> stacks, final FluidStack required) {
        if (!fluidPlanReady || fluids.drained != required) {
            clearFluidPlan();
            fluids.collect(stacks, required);
            fluidPlanReady = true;
        }
        return fluids.remaining;
    }

    private void clearFluidPlan() {
        fluidPlanReady = false;
        fluids.reset();
    }

    static final class Recipe {
        private static final Option[] NO_OPTIONS = new Option[0];
        private final @Nullable BlockInformation tileMatcher;
        private final IBlockState sample;
        private final boolean acceptsAir;
        private final boolean fluidOnly;
        private final boolean staticOnly;
        private final long firstState;
        private final @Nullable LongArraySet otherStates;
        private final Option[] options;

        Recipe(@Nullable final BlockInformation tileMatcher, @Nullable final IBlockState sample, final boolean acceptsAir,
               final boolean fluidOnly, final boolean staticOnly, final long firstState,
               @Nullable final LongArraySet otherStates, final Option[] options) {
            this.tileMatcher = tileMatcher;
            this.sample = sample;
            this.acceptsAir = acceptsAir;
            this.fluidOnly = fluidOnly;
            this.staticOnly = staticOnly;
            this.firstState = firstState;
            this.otherStates = otherStates;
            this.options = options;
        }

        @Nullable
        IBlockState sample() {
            return sample;
        }

        @Nullable
        IBlockState waitingState() {
            return options.length == 0 ? null : options[0].state;
        }

        boolean fluidOnly() {
            return fluidOnly;
        }

        boolean staticOnly() {
            return staticOnly;
        }

        boolean matches(final WorldServer world, final BlockPos pos, final IBlockState state) {
            if (!acceptsAir && state.getBlock() == Blocks.AIR) return false;
            if (tileMatcher != null) return tileMatcher.matchesState(world, pos, state);
            if (state == sample) return true;
            final long id = stateKey(state);
            return id == firstState || otherStates != null && otherStates.contains(id);
        }
    }

    private record Option(IBlockState state, @Nullable ItemKey item, @Nullable FluidMaterial fluid) {
    }

    private static final class FluidMaterial {
        final FluidStack stack;
        final IAEFluidStack ae;
        private IAEItemStack dropKey;

        FluidMaterial(final FluidStack stack, final IAEFluidStack ae) {
            this.stack = stack;
            this.ae = ae;
        }

        IAEItemStack dropKey() {
            if (dropKey == null) dropKey = FakeFluids.packFluid2AEDrops(ae);
            return dropKey;
        }
    }

    private static final class ItemKey {
        private final ItemStack stack;
        private IAEItemStack ae;
        private ItemKey next;
        private int slot = -1;
        private int checkedScan = -1;

        ItemKey(final ItemStack stack) {
            this.stack = stack;
        }

        int slot(final AssemblyInventory inventory, final int scan) {
            if (slot >= 0 && slot < inventory.size() && ItemUtils.matchStacks(stack, inventory.get(slot))
                && !inventory.extract(slot, 1, true).isEmpty()) return slot;
            if (slot < 0 && checkedScan == scan) return -1;
            checkedScan = scan;
            for (int i = 0; i < inventory.size(); i++) {
                final var current = inventory.get(i);
                if (!current.isEmpty() && ItemUtils.matchStacks(stack, current) && !inventory.extract(i, 1, true).isEmpty())
                    return slot = i;
            }
            return slot = -1;
        }

    }

    private final class MissingReport {
        private static final int DISPLAYED_ALTERNATIVES = 8;
        private final Preflight preview;
        private final Reference2IntLinkedOpenHashMap<ItemKey> items = new Reference2IntLinkedOpenHashMap<>(4);
        private final Reference2IntLinkedOpenHashMap<FluidMaterial> liquids = new Reference2IntLinkedOpenHashMap<>(4);
        private final Reference2IntLinkedOpenHashMap<Recipe> alternatives = new Reference2IntLinkedOpenHashMap<>(4);
        private final Reference2LongOpenHashMap<FluidMaterial> liquidCredits = new Reference2LongOpenHashMap<>(4);
        private final Reference2LongOpenHashMap<FluidMaterial> liquidCreditSpent = new Reference2LongOpenHashMap<>(4);
        private ObjectIterator<Reference2IntMap.Entry<ItemKey>> itemEntries;
        private ObjectIterator<Reference2IntMap.Entry<FluidMaterial>> liquidEntries;
        private ObjectIterator<Reference2IntMap.Entry<Recipe>> alternativeEntries;
        private IGrid craftGrid;
        private ICraftingGrid crafting;
        private Object2IntLinkedOpenHashMap<IAEItemStack> quantities;
        private ObjectIterator<Object2IntMap.Entry<IAEItemStack>> craftEntries;
        private ArrayDeque<ItemStack> queue;
        private Recipe choice;
        private ITextComponent line;
        private int choiceIndex, choiceCount, unavailable;
        private boolean chosen;

        MissingReport(final Preflight preview) {
            this.preview = preview;
            liquidCredits.defaultReturnValue(-1L);
            liquidCreditSpent.defaultReturnValue(0L);
        }

        void refreshQuotes() {
            liquidCredits.clear();
        }

        void add(final Recipe recipe) {
            if (recipe.options.length == 0) unavailable++;
            else if (recipe.options.length > 1) alternatives.addTo(recipe, 1);
            else {
                final var option = recipe.options[0];
                if (option.item != null) items.addTo(option.item, 1);
                else liquids.addTo(option.fluid, 1);
            }
        }

        boolean step() {
            if (itemEntries == null) {
                itemEntries = items.reference2IntEntrySet().fastIterator();
                liquidEntries = liquids.reference2IntEntrySet().fastIterator();
                alternativeEntries = alternatives.reference2IntEntrySet().fastIterator();
                if (autoCraft) {
                    resolveME();
                    craftGrid = grid;
                    if (craftGrid != null) crafting = craftGrid.getCache(ICraftingGrid.class);
                }
                player.sendMessage(new TextComponentTranslation("message.assembly.tip.required"));
                return false;
            }
            if (itemEntries.hasNext()) {
                final var entry = itemEntries.next();
                final var key = entry.getKey();
                final int amount = entry.getIntValue();
                player.sendMessage(itemLine(key, amount));
                offer(key.ae, amount);
                return false;
            }
            if (liquidEntries.hasNext()) {
                final var entry = liquidEntries.next();
                final var key = entry.getKey();
                final int blocks = entry.getIntValue();
                final int amount = liquidShortage(key, blocks, true);
                player.sendMessage(liquidLine(key, amount));
                if (crafting != null) offer(key.dropKey(), amount);
                return false;
            }
            if (choice != null) {
                advanceChoice();
                return false;
            }
            if (alternativeEntries.hasNext()) {
                final var entry = alternativeEntries.next();
                choice = entry.getKey();
                choiceCount = entry.getIntValue();
                choiceIndex = 0;
                chosen = false;
                return false;
            }
            if (unavailable != 0) {
                player.sendMessage(new TextComponentTranslation("message.assembly.tip.material_unavailable", unavailable));
                unavailable = 0;
                return false;
            }
            if (quantities == null) return true;
            if (craftEntries == null) {
                craftEntries = quantities.object2IntEntrySet().fastIterator();
                queue = new ArrayDeque<>(quantities.size());
            }
            if (craftEntries.hasNext()) {
                final var entry = craftEntries.next();
                final var stack = entry.getKey().createItemStack();
                stack.setCount(entry.getIntValue());
                queue.addLast(stack);
                return false;
            }
            resolveME();
            if (grid == craftGrid) AutoCraftingQueue.setQueueAndStrat(queue, player);
            return true;
        }

        private void advanceChoice() {
            final int size = choice.options.length;
            final boolean display = choiceIndex < DISPLAYED_ALTERNATIVES;
            final boolean auto = crafting != null && !chosen;
            if (choiceIndex == size || !display && !auto) {
                if (size > DISPLAYED_ALTERNATIVES) {
                    line.appendSibling(new TextComponentTranslation("message.assembly.tip.material_more", size - DISPLAYED_ALTERNATIVES));
                }
                player.sendMessage(line);
                line = null;
                choice = null;
                return;
            }
            final var option = choice.options[choiceIndex++];
            if (option.item != null) {
                if (display) append(itemLine(option.item, choiceCount));
                if (auto) chosen = offer(option.item.ae, choiceCount);
            } else {
                final var key = option.fluid;
                final int amount = liquidShortage(key, choiceCount, false);
                if (display) append(liquidLine(key, amount));
                if (auto && offer(key.dropKey(), amount)) {
                    liquidShortage(key, choiceCount, true);
                    chosen = true;
                }
            }
            return;
        }

        private int liquidShortage(final FluidMaterial key, final int blocks, final boolean consumeCredit) {
            long credit = liquidCredits.getLong(key);
            if (credit < 0L) {
                credit = Math.max(0L, preview.remainingFluid(key) - liquidCreditSpent.getLong(key));
                liquidCredits.put(key, credit);
            }
            final long required = (long) key.stack.amount * blocks;
            final long used = Math.min(credit, required - 1L);
            if (consumeCredit) {
                liquidCredits.put(key, credit - used);
                liquidCreditSpent.addTo(key, used);
            }
            return (int) (required - used);
        }

        private boolean offer(@Nullable final IAEItemStack key, final int amount) {
            if (crafting == null || key == null || crafting.getCraftingFor(key, null, 0, player.world).isEmpty())
                return false;
            if (quantities == null) quantities = new Object2IntLinkedOpenHashMap<>(4);
            quantities.addTo(key, amount);
            return true;
        }

        private void append(final ITextComponent component) {
            if (line == null) line = component;
            else line.appendText(" / ").appendSibling(component);
        }
    }

    private static ITextComponent itemLine(final ItemKey key, final int amount) {
        final var stack = key.stack;
        final ITextComponent name = stack.hasDisplayName() ? new TextComponentString(stack.getDisplayName())
            : new TextComponentTranslation(stack.getItem().getTranslationKey(stack) + ".name");
        return new TextComponentTranslation("message.assembly.tip.material_item", name, amount);
    }

    private static ITextComponent liquidLine(final FluidMaterial key, final int amount) {
        return new TextComponentTranslation("message.assembly.tip.material_fluid",
            new TextComponentTranslation(key.stack.getTranslationKey()), amount);
    }

    private final class Preflight {
        private static final long NOT_QUOTED = Long.MIN_VALUE;
        private final Object2LongOpenHashMap<IAEStack<?>> debitedME = new Object2LongOpenHashMap<>();
        private final Object2LongOpenHashMap<IAEStack<?>> availableME = new Object2LongOpenHashMap<>();
        private final Object2LongOpenHashMap<IAEStack<?>> requestedME = new Object2LongOpenHashMap<>();
        private final Reference2IntOpenHashMap<ItemKey> itemStock = new Reference2IntOpenHashMap<>();
        private MissingReport missing;
        private final ItemStack[] originals = hotbarAndStorage.toArray(ItemStack[]::new);
        private final int[] remaining = new int[originals.length];
        private final boolean[] writable = new boolean[originals.length];
        private ObjectArrayList<ItemStack> fluidInventory;
        private int[] fluidSlots;
        private IntArrayList fluidOrigins;

        Preflight() {
            for (int i = 0; i < originals.length; i++) {
                final var stack = originals[i];
                writable[i] = hotbarAndStorage.writable(i);
                final var extracted = stack.isEmpty() ? ItemStack.EMPTY : hotbarAndStorage.extract(i, stack.getCount(), true);
                remaining[i] = ItemUtils.matchStacks(stack, extracted) ? Math.min(stack.getCount(), extracted.getCount()) : 0;
                if (!stack.isEmpty()) adjustStock(stack, remaining[i]);
            }
        }

        void missing(final Recipe recipe) {
            if (missing == null) missing = new MissingReport(this);
            missing.add(recipe);
        }

        boolean takeItem(final ItemKey key) {
            if (itemStock.getInt(key) == 0) return false;
            for (int i = 0; i < remaining.length; i++) {
                if (remaining[i] == 0) continue;
                final int virtualSlot = fluidSlots == null ? -1 : fluidSlots[i];
                final var current = virtualSlot < 0 ? originals[i] : fluidInventory.get(virtualSlot);
                if (!ItemUtils.matchStacks(key.stack, current)) continue;
                itemStock.addTo(key, -1);
                final int after = --remaining[i];
                if (virtualSlot >= 0) {
                    if (after == 0) fluidInventory.set(virtualSlot, ItemStack.EMPTY);
                    else current.shrink(1);
                }
                return true;
            }
            return false;
        }

        int takeFluid(final FluidMaterial material, final boolean network) {
            if (fluidInventory == null) captureFluidInventory();
            int remainder = collectFluid(fluidInventory, material.stack);
            if (remainder != 0 && network && takeFromME(material.ae, remainder)) remainder = 0;
            if (remainder == 0) {
                applyFluidEdits();
                clearFluidPlan();
            }
            return remainder;
        }

        private void captureFluidInventory() {
            final var capability = CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
            fluidInventory = new ObjectArrayList<>();
            fluidSlots = new int[originals.length];
            Arrays.fill(fluidSlots, -1);
            fluidOrigins = new IntArrayList();
            for (int i = 0; i < originals.length; i++) {
                final var stack = originals[i];
                if (remaining[i] == 0 || !writable[i] || !stack.hasCapability(capability, null)) continue;
                fluidSlots[i] = fluidInventory.size();
                fluidOrigins.add(i);
                final var working = stack.copy();
                working.setCount(remaining[i]);
                fluidInventory.add(working);
            }
        }

        private void applyFluidEdits() {
            for (int i = 0; i < fluids.edits; i++) {
                final int slot = fluids.slots[i];
                final var previous = fluids.before[i];
                final var after = fluids.after[i];
                fluidInventory.set(slot, after);
                adjustStock(previous, -previous.getCount());
                adjustStock(after, after.getCount());
                remaining[fluidOrigins.getInt(slot)] = after.getCount();
            }
        }

        private void adjustStock(final ItemStack stack, final int amount) {
            for (var key = indexedItems.get(stack.getItem()); key != null; key = key.next) {
                if (ItemUtils.matchStacks(key.stack, stack)) itemStock.addTo(key, amount);
            }
        }

        boolean takeFromME(@Nullable final IAEStack<?> key, final int amount) {
            if (key == null) return false;
            final long total = debitedME.getLong(key) + amount;
            final long known = availableME.getOrDefault(key, NOT_QUOTED);
            long limit = known == NOT_QUOTED ? 0L : Math.abs(known);
            // A negative entry is a fully supplied quote. The confirmed quantity can grow on demand.
            if (known == NOT_QUOTED || known < 0 && total > limit) {
                final long quote = Math.max(total * 2, key.isItem() ? 64 : 64_000);
                requestedME.put(key, Math.max(requestedME.getLong(key), quote));
                limit = simulate(key, quote);
                availableME.put(key, limit == quote ? -quote : limit);
            }
            if (limit < total) return false;
            debitedME.put(key, total);
            return true;
        }

        long remainingFluid(final FluidMaterial material) {
            clearFluidPlan();
            long quoted = availableME.getOrDefault(material.ae, NOT_QUOTED);
            if (quoted == NOT_QUOTED) {
                final long request = requestedME.getLong(material.ae);
                if (request > 0L) {
                    final long limit = simulate(material.ae, request);
                    availableME.put(material.ae, limit == request ? -request : limit);
                    quoted = availableME.getLong(material.ae);
                } else {
                    quoted = 0L;
                }
            }
            long available = Math.max(0L, Math.abs(quoted) - debitedME.getLong(material.ae));
            if (fluidInventory != null) {
                final int unfilled = collectFluid(fluidInventory, material.stack);
                available += material.stack.amount - unfilled;
                clearFluidPlan();
            }
            return available;
        }
    }

    /**
     * Build edits on isolated one-item capabilities; no live container is drained before payment succeeds.
     */
    private static final class FluidPlan {
        private int[] slots = new int[4];
        private int[] amounts = new int[4];
        private int[] returnStarts = new int[5];
        private ItemStack[] before = new ItemStack[4];
        private ItemStack[] after = new ItemStack[4];
        private ItemStack[] written = new ItemStack[4];
        private final ObjectArrayList<ItemStack> returned = new ObjectArrayList<>();
        private int edits;
        private int remaining;
        private FluidStack drained;

        void collect(final List<ItemStack> inventory, final FluidStack needed) {
            final var capability = CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
            int remaining = needed.amount;
            drained = needed;
            for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
                if (inventory instanceof AssemblyInventory sources && !sources.writable(slot)) continue;
                final var original = inventory.get(slot);
                if (original.isEmpty() || !original.hasCapability(capability, null)) continue;
                final int count = original.getCount();
                ItemStack replacement = ItemStack.EMPTY;
                final int start = returned.size();
                int used = 0, amount = remaining;
                while (used < count && remaining > 0) {
                    final var single = original.copy();
                    single.setCount(1);
                    final IFluidHandlerItem handler = single.getCapability(capability, null);
                    if (handler == null) break;
                    final var request = new FluidStack(needed, remaining);
                    final var actual = handler.drain(request, true);
                    if (actual == null || actual.amount <= 0 || actual.amount > remaining || !needed.isFluidEqual(actual))
                        break;
                    remaining -= actual.amount;
                    used++;
                    replacement = handler.getContainer();
                    if (!replacement.isEmpty()) returned.add(replacement);
                }
                if (used == 0) continue;
                if (used == count) {
                    if (!replacement.isEmpty()) returned.removeLast();
                } else {
                    replacement = original.copy();
                    replacement.setCount(count - used);
                }
                add(slot, original, replacement, amount - remaining, start);
            }
            this.remaining = remaining;
        }

        private void add(final int slot, final ItemStack original, final ItemStack replacement, final int amount, final int start) {
            if (edits == slots.length) {
                final int capacity = slots.length * 2;
                slots = Arrays.copyOf(slots, capacity);
                amounts = Arrays.copyOf(amounts, capacity);
                returnStarts = Arrays.copyOf(returnStarts, capacity + 1);
                before = Arrays.copyOf(before, capacity);
                after = Arrays.copyOf(after, capacity);
                written = Arrays.copyOf(written, capacity);
            }
            slots[edits] = slot;
            amounts[edits] = amount;
            returnStarts[edits] = start;
            before[edits] = original;
            after[edits] = replacement;
            returnStarts[++edits] = returned.size();
        }

        void reset() {
            Arrays.fill(before, 0, edits, null);
            Arrays.fill(after, 0, edits, null);
            Arrays.fill(written, 0, edits, null);
            returned.clear();
            edits = 0;
            drained = null;
        }
    }
}
