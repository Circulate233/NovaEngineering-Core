package github.kasuminova.novaeng.common.tile.ecotech.efabricator;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import appeng.util.item.AEItemStack;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.block.ecotech.efabricator.prop.WorkerStatus;
import github.kasuminova.novaeng.common.network.PktEFabricatorWorkerStatusUpdate;
import github.kasuminova.novaeng.common.performance.ItemStackIndex;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public final class EFabricatorWorker extends EFabricatorPart {
    public static final int MAX_ENERGY_CACHE = 500000;
    public static final int MAX_QUEUE_DEPTH = 32;
    public static final int ENERGY_USAGE = 100;
    public static final int COOLANT_USAGE = 5;
    private static final WorkerStatus[] STATUSES = WorkerStatus.values();
    private final CraftingQueue queue = new CraftingQueue();
    private WorkerStatus status = WorkerStatus.OFF;
    private int queueDepth = MAX_QUEUE_DEPTH;
    private int energyCache;
    private long updateTick;

    public CraftingQueue getQueue() {
        return this.queue;
    }

    public WorkerStatus getStatus() {
        return this.status;
    }

    public void setStatus(final WorkerStatus status) {
        this.status = status;
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            this.markNoUpdateSync();
        }
    }

    public int getQueueDepth() {
        final var controller = this.getController();
        return controller != null && controller.getOverclocked()
            ? controller.getLevel().applyOverclockQueueDepth(this.queueDepth) : this.queueDepth;
    }

    public void setQueueDepth(final int depth) {
        this.queueDepth = depth;
    }

    public int getEnergyCache() {
        return this.energyCache;
    }

    public void setEnergyCache(final int energy) {
        this.energyCache = energy;
    }

    public long getUpdateTick() {
        return this.updateTick;
    }

    public void setUpdateTick(final long tick) {
        this.updateTick = tick;
    }

    public int getRemainingSpace() {
        return this.getQueueDepth() - this.queue.size();
    }

    public int getMaxEnergyCache() {
        return MAX_ENERGY_CACHE;
    }

    public synchronized int doWork() {
        final var controller = this.partController;
        final int coolant = controller.getCoolantCache();
        final int usage = controller.getOverclocked() && !controller.getActiveCooling()
            ? controller.getLevel().applyOverclockEnergyUsage(ENERGY_USAGE) : ENERGY_USAGE;
        double energy = this.energyCache;
        IEnergyGrid grid = null;
        final var channel = controller.getChannel();
        if (channel != null) {
            try {
                grid = channel.getProxy().getGrid().getCache(IEnergyGrid.class);
                energy += grid.getStoredPower();
            } catch (final GridAccessException ignored) {
            }
        }
        int parallelism = (int) Math.min(Math.max(controller.getAvailableParallelism(), 1), energy / usage);
        if (controller.getActiveCooling()) {
            parallelism = Math.min(parallelism, coolant / COOLANT_USAGE);
        }
        int completed = 0;
        final IItemList<IAEItemStack> output = controller.getOutputBuffer();
        synchronized (output) {
            while (parallelism > completed && !this.queue.isEmpty()) {
                final CraftWork work = this.queue.poll();
                final int available = parallelism - completed;
                final CraftWork finished;
                if (work.size > available) {
                    finished = work.split(available);
                    this.queue.add(work);
                } else {
                    finished = work;
                }
                for (final ItemStack remaining : finished.remaining) {
                    if (!remaining.isEmpty()) {
                        output.add(AEItemStack.fromItemStack(remaining));
                    }
                }
                output.add(AEItemStack.fromItemStack(finished.output));
                completed += finished.size;
            }
        }
        if (completed > 0) {
            int used = usage * completed;
            if (used > this.energyCache) {
                used -= this.energyCache;
                this.energyCache = 0;
                if (grid != null) {
                    synchronized (grid) {
                        grid.injectPower(used, Actionable.MODULATE);
                    }
                }
            } else {
                this.energyCache -= used;
            }
            if (controller.getActiveCooling()) {
                controller.consumeCoolant(COOLANT_USAGE * completed);
            }
        }
        return completed;
    }

    public void supplyEnergy(final int energy) {
        this.energyCache += energy;
    }

    public void offerWork(final CraftWork work) {
        this.queue.add(work);
    }

    public boolean hasWork() {
        return !this.queue.isEmpty();
    }

    public boolean isFull() {
        return this.queue.size() >= this.getQueueDepth();
    }

    @Override
    public void onAssembled() {
        this.updateStatus(true);
        super.onAssembled();
    }

    @Override
    public void onDisassembled() {
        this.updateStatus(true);
        super.onDisassembled();
    }

    public void updateStatus(final boolean force) {
        final long tick = this.getWorld().getTotalWorldTime();
        if (!force && this.status == WorkerStatus.RUN && this.updateTick + 20 >= tick) {
            if (this.hasWork()) {
                this.updateTick = tick;
            }
            return;
        }
        final WorkerStatus next = this.getController() == null ? WorkerStatus.OFF : this.hasWork() ? WorkerStatus.RUN : WorkerStatus.ON;
        if (this.status != next) {
            this.setStatus(next);
        }
        this.updateTick = tick;
    }

    @Override
    public void markNoUpdate() {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            NovaEngineeringCore.NET_CHANNEL.sendToAllTracking(new PktEFabricatorWorkerStatusUpdate(this.getPos(), this.status),
                new NetworkRegistry.TargetPoint(this.world.provider.getDimension(), this.pos.getX(), this.pos.getY(), this.pos.getZ(), -1));
        }
        super.markNoUpdate();
    }

    @Override
    public void readCustomNBT(final NBTTagCompound tag) {
        this.queue.readFromNBT(tag);
        this.energyCache = tag.getInteger("energyCache");
        this.setStatus(STATUSES[tag.getByte("status")]);
        super.readCustomNBT(tag);
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound tag) {
        this.queue.writeToNBT(tag);
        tag.setInteger("energyCache", this.energyCache);
        tag.setByte("status", (byte) this.status.ordinal());
        super.writeCustomNBT(tag);
    }

    public static final class CraftingQueue {
        private final Deque<CraftWork> queue = new ArrayDeque<>();
        private int size;

        public Deque<CraftWork> getQueue() {
            return this.queue;
        }

        public int size() {
            return this.size;
        }

        public boolean isEmpty() {
            return this.queue.isEmpty();
        }

        public void add(final CraftWork work) {
            this.queue.add(work);
            this.size += work.size;
        }

        public CraftWork poll() {
            final CraftWork work = this.queue.poll();
            this.size = work == null ? 0 : this.size - work.size;
            return work;
        }

        public CraftWork peek() {
            return this.queue.peek();
        }

        public NBTTagCompound writeToNBT(final NBTTagCompound tag) {
            if (this.queue.isEmpty()) {
                return tag;
            }
            final List<ItemStack> stacks = new ObjectArrayList<>();
            final NBTTagList workTags = new NBTTagList();
            CraftWork previous = null;
            int repeat = 0;
            try (final ItemStackIndex stackIndex = this.shouldIndexStacks() ? ItemStackIndex.open(stacks, true) : null) {
                for (final CraftWork work : this.queue) {
                    if (previous != null && previous.equals(work)) {
                        repeat++;
                        continue;
                    }
                    if (repeat > 0) {
                        workTags.getCompoundTagAt(workTags.tagCount() - 1).setShort("R", (short) repeat);
                        repeat = 0;
                    }
                    workTags.appendTag(work.writeToNBT(stacks, stackIndex));
                    previous = work;
                }
            }
            if (repeat > 0) {
                workTags.getCompoundTagAt(workTags.tagCount() - 1).setShort("R", (short) repeat);
            }
            tag.setTag("Q", workTags);
            final NBTTagCompound stackTags = new NBTTagCompound();
            for (int i = 0; i < stacks.size(); i++) {
                if (!stacks.get(i).isEmpty()) {
                    stackTags.setTag("S#" + i, stacks.get(i).serializeNBT());
                }
            }
            tag.setTag("SS", stackTags);
            tag.setInteger("SSS", stacks.size());
            return tag;
        }

        private boolean shouldIndexStacks() {
            Item first = null;
            boolean different = false;
            int checked = 0;
            for (final CraftWork work : this.queue) {
                for (int i = -1; i < work.remaining.length; i++) {
                    final ItemStack stack = i < 0 ? work.output : work.remaining[i];
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (first == null) {
                        first = stack.getItem();
                    } else if (first != stack.getItem()) {
                        different = true;
                    }
                    if (++checked == ItemStackIndex.INDEX_THRESHOLD) {
                        return different;
                    }
                }
            }
            return false;
        }

        public void readFromNBT(final NBTTagCompound tag) {
            this.queue.clear();
            final List<ItemStack> stacks = new ObjectArrayList<>();
            final NBTTagCompound stackTags = tag.getCompoundTag("SS");
            for (int i = 0; i < tag.getInteger("SSS"); i++) {
                stacks.add(new ItemStack(stackTags.getCompoundTag("S#" + i)));
            }
            final NBTTagList workTags = tag.getTagList("Q", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < workTags.tagCount(); i++) {
                final NBTTagCompound workTag = workTags.getCompoundTagAt(i);
                final CraftWork work = new CraftWork(workTag, stacks);
                this.queue.add(work);
                for (int repeat = 0; repeat < workTag.getShort("R"); repeat++) {
                    this.queue.add(work.copy());
                }
            }
        }
    }

    public static final class CraftWork {
        private final ItemStack[] remaining;
        private final ItemStack output;
        private int size;

        public CraftWork(final ItemStack[] remaining, final ItemStack output, final int size) {
            this.remaining = remaining;
            this.output = output;
            this.size = Math.max(1, size);
        }

        public CraftWork(final NBTTagCompound tag, final List<ItemStack> stacks) {
            this.remaining = new ItemStack[tag.getByte("R#S")];
            for (int i = 0; i < this.remaining.length; i++) {
                final int index = tag.hasKey("R#" + i) ? tag.getInteger("R#" + i) : -1;
                this.remaining[i] = index == -1 ? ItemStack.EMPTY : stacks.get(index);
            }
            this.output = stacks.get(tag.getInteger("O"));
            this.size = Math.max(1, tag.getInteger("Z"));
        }

        private static boolean matchStacksStrict(final ItemStack first, final ItemStack second) {
            return ItemUtils.matchStacks(first, second) && first.getCount() == second.getCount();
        }

        public ItemStack[] getRemaining() {
            return this.remaining;
        }

        public ItemStack getOutput() {
            return this.output;
        }

        public int getSize() {
            return this.size;
        }

        public void setSize(final int size) {
            this.size = size;
        }

        public CraftWork split(final int amount) {
            final int count = Math.min(amount, this.size);
            final ItemStack[] inputs = new ItemStack[this.remaining.length];
            if (count <= 0) {
                Arrays.fill(inputs, ItemStack.EMPTY);
                return new CraftWork(inputs, ItemStack.EMPTY, 0);
            }
            for (int i = 0; i < inputs.length; i++) {
                inputs[i] = this.remaining[i].splitStack(count);
            }
            final ItemStack result = this.output.copy();
            final int outputCount = count * (this.output.getCount() / this.size);
            result.setCount(outputCount);
            this.output.shrink(outputCount);
            this.size -= count;
            return new CraftWork(inputs, result, count);
        }

        public NBTTagCompound writeToNBT(final List<ItemStack> stacks) {
            if (stacks.size() + this.remaining.length + 1 < ItemStackIndex.INDEX_THRESHOLD) {
                return this.writeToNBT(stacks, null);
            }
            try (final ItemStackIndex stackIndex = ItemStackIndex.open(stacks, true)) {
                return this.writeToNBT(stacks, stackIndex);
            }
        }

        private NBTTagCompound writeToNBT(final List<ItemStack> stacks, final ItemStackIndex stackIndex) {
            if (stackIndex == null) {
                return this.writeLinearNBT(stacks);
            }
            final NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("R#S", (byte) this.remaining.length);
            for (int i = 0; i < this.remaining.length; i++) {
                final ItemStack stack = this.remaining[i];
                if (!stack.isEmpty()) {
                    tag.setShort("R#" + i, (short) stackIndex.findOrAdd(stack));
                }
            }
            final int outputIndex = stackIndex.find(this.output);
            if (outputIndex >= 0) {
                tag.setShort("O", (short) outputIndex);
                return tag;
            }
            stacks.add(this.output);
            tag.setShort("O", (short) (stacks.size() - 1));
            tag.setInteger("Z", this.size);
            return tag;
        }

        private NBTTagCompound writeLinearNBT(final List<ItemStack> stacks) {
            final NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("R#S", (byte) this.remaining.length);
            remainingLoop:
            for (int i = 0; i < this.remaining.length; i++) {
                final ItemStack stack = this.remaining[i];
                if (stack.isEmpty()) {
                    continue;
                }
                for (int j = 0; j < stacks.size(); j++) {
                    if (matchStacksStrict(stack, stacks.get(j))) {
                        tag.setShort("R#" + i, (short) j);
                        continue remainingLoop;
                    }
                }
                stacks.add(stack);
                tag.setShort("R#" + i, (short) (stacks.size() - 1));
            }
            for (int i = 0; i < stacks.size(); i++) {
                if (matchStacksStrict(this.output, stacks.get(i))) {
                    tag.setShort("O", (short) i);
                    return tag;
                }
            }
            stacks.add(this.output);
            tag.setShort("O", (short) (stacks.size() - 1));
            tag.setInteger("Z", this.size);
            return tag;
        }

        public CraftWork copy() {
            final ItemStack[] copy = new ItemStack[this.remaining.length];
            for (int i = 0; i < copy.length; i++) {
                copy[i] = this.remaining[i].copy();
            }
            return new CraftWork(copy, this.output.copy(), this.size);
        }

        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof CraftWork work)) {
                return false;
            }
            for (int i = 0; i < this.remaining.length; i++) {
                if (!matchStacksStrict(this.remaining[i], work.remaining[i])) {
                    return false;
                }
            }
            return matchStacksStrict(this.output, work.output);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * this.size + Arrays.hashCode(this.remaining)) + this.output.hashCode();
        }
    }
}
