package github.kasuminova.novaeng.common.tile.ecotech.efabricator;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import appeng.util.item.ItemList;
import github.kasuminova.mmce.client.util.ItemStackUtils;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.client.util.BlockModelHider;
import github.kasuminova.novaeng.common.block.ecotech.efabricator.BlockEFabricatorController;
import github.kasuminova.novaeng.common.block.ecotech.efabricator.prop.Levels;
import github.kasuminova.novaeng.common.network.PktEFabricatorGUIData;
import github.kasuminova.novaeng.common.tile.ecotech.EPartController;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorWorker.CraftWork;
import github.kasuminova.novaeng.common.util.Functions;
import github.kasuminova.novaeng.common.util.MachineCoolants;
import hellfirepvp.modularmachinery.client.ClientProxy;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.List;

public final class EFabricatorController extends EPartController<EFabricatorPart> {
    public static final int MAX_COOLANT_CACHE = 100000;
    public static final int WORK_DELAY = 20;
    public static final List<BlockPos> HIDE_POS_LIST = Functions.asList(
        new BlockPos(0, 1, 0), new BlockPos(0, -1, 0), new BlockPos(0, 1, 1), new BlockPos(0, 0, 1), new BlockPos(0, -1, 1),
        new BlockPos(1, 1, 0), new BlockPos(1, 0, 0), new BlockPos(1, -1, 0), new BlockPos(1, 1, 1), new BlockPos(1, 0, 1), new BlockPos(1, -1, 1),
        new BlockPos(-1, 1, 0), new BlockPos(-1, 0, 0), new BlockPos(-1, -1, 0), new BlockPos(-1, 1, 1), new BlockPos(-1, 0, 1), new BlockPos(-1, -1, 1));
    private final ObjectArrayList<IFluidHandler> coolantInputHandlers = new ObjectArrayList<>();
    private final ObjectArrayList<IFluidHandler> coolantOutputHandlers = new ObjectArrayList<>();
    private final IItemStorageChannel itemChannel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
    private IItemList<IAEItemStack> outputBuffer = new ItemList();
    private double energyConsumePerTick = 64.0;
    private EFabricatorMEChannel channel;
    private int length;
    private int workDelay = WORK_DELAY;
    private int maxWorkDelay = WORK_DELAY;
    private int parallelism;
    private int consumedParallelism;
    private int coolantCache;
    private long totalCrafted;
    private boolean speedupApplied;
    private boolean overclocked;
    private boolean activeCooling;
    private PktEFabricatorGUIData guiDataPacket;
    private volatile boolean guiDataDirty;

    public EFabricatorController() {
        this.workMode = WorkMode.SEMI_SYNC;
    }

    public EFabricatorController(final ResourceLocation machine) {
        this();
        this.parentMachine = MachineRegistry.getRegistry().getMachine(machine);
    }

    private static int roundToInt(final double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Cannot round NaN value.");
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) Math.round(value);
    }

    private static int capacity(final List<IFluidHandler> handlers) {
        int total = 0;
        for (final var handler : handlers) {
            for (final var property : handler.getTankProperties()) {
                total += Math.min(property.getCapacity(), Integer.MAX_VALUE - total);
                if (total == Integer.MAX_VALUE) {
                    return total;
                }
            }
        }
        return total;
    }

    public ObjectArrayList<IFluidHandler> getCoolantInputHandlers() {
        return this.coolantInputHandlers;
    }

    public ObjectArrayList<IFluidHandler> getCoolantOutputHandlers() {
        return this.coolantOutputHandlers;
    }

    public IItemStorageChannel getItemChannel() {
        return this.itemChannel;
    }

    public IItemList<IAEItemStack> getOutputBuffer() {
        return this.outputBuffer;
    }

    public void setOutputBuffer(final IItemList<IAEItemStack> buffer) {
        this.outputBuffer = buffer;
    }

    @Nullable
    public BlockEFabricatorController getParentController() {
        return this.getBlockType() instanceof BlockEFabricatorController block ? block : null;
    }

    public double getEnergyConsumePerTick() {
        return this.energyConsumePerTick;
    }

    public void setEnergyConsumePerTick(final double energy) {
        this.energyConsumePerTick = energy;
    }

    @Nullable
    public EFabricatorMEChannel getChannel() {
        return this.channel;
    }

    public void setChannel(@Nullable final EFabricatorMEChannel channel) {
        this.channel = channel;
    }

    public int getLength() {
        return this.length;
    }

    public void setLength(final int length) {
        this.length = length;
    }

    public int getWorkDelay() {
        return this.workDelay;
    }

    public void setWorkDelay(final int delay) {
        this.workDelay = delay;
    }

    public int getMaxWorkDelay() {
        return this.maxWorkDelay;
    }

    public void setMaxWorkDelay(final int delay) {
        this.maxWorkDelay = delay;
    }

    public int getParallelism() {
        return this.parallelism;
    }

    public void setParallelism(final int parallelism) {
        this.parallelism = parallelism;
    }

    public int getConsumedParallelism() {
        return this.consumedParallelism;
    }

    public void setConsumedParallelism(final int parallelism) {
        this.consumedParallelism = parallelism;
    }

    public int getCoolantCache() {
        return this.coolantCache;
    }

    public void setCoolantCache(final int coolant) {
        this.coolantCache = coolant;
    }

    public long getTotalCrafted() {
        return this.totalCrafted;
    }

    public void setTotalCrafted(final long total) {
        this.totalCrafted = total;
    }

    public boolean getSpeedupApplied() {
        return this.speedupApplied;
    }

    public void setSpeedupApplied(final boolean applied) {
        this.speedupApplied = applied;
    }

    public boolean getOverclocked() {
        return this.overclocked;
    }

    public void setOverclocked(final boolean overclocked) {
        this.overclocked = overclocked;
        this.updateParallelism();
        this.updateGUIDataPacket();
    }

    public boolean getActiveCooling() {
        return this.activeCooling;
    }

    public void setActiveCooling(final boolean cooling) {
        this.activeCooling = cooling;
        this.updateParallelism();
        this.updateWorkDelay();
        this.updateGUIDataPacket();
    }

    public PktEFabricatorGUIData getGuiDataPacket() {
        if (this.guiDataDirty || this.guiDataPacket == null) {
            this.guiDataPacket = new PktEFabricatorGUIData(this);
            this.guiDataDirty = false;
        }
        return this.guiDataPacket;
    }

    public void setGuiDataPacket(final PktEFabricatorGUIData packet) {
        this.guiDataPacket = packet;
    }

    public boolean getGuiDataDirty() {
        return this.guiDataDirty;
    }

    public void setGuiDataDirty(final boolean dirty) {
        this.guiDataDirty = dirty;
    }

    @Override
    public boolean onSyncTick() {
        if (this.channel == null || !this.channel.getProxy().isActive()) {
            this.tickExecutor = null;
            return false;
        }
        if (--this.workDelay > 0) {
            this.tickExecutor = null;
            return false;
        }
        this.workDelay = this.maxWorkDelay;
        this.speedupApplied = false;
        this.clearOutputBuffer();
        this.supplyWorkerPower();
        this.supplyCoolantCache();
        return true;
    }

    @Override
    public void onAsyncTick() {
        this.updateGUIDataPacket();
        final long previous = this.totalCrafted;
        final var workers = this.getWorkers();
        workers.forEach(worker -> worker.updateStatus(false));
        for (final var worker : workers) {
            if (worker.hasWork()) {
                final int worked = worker.doWork();
                this.totalCrafted += worked;
                this.consumedParallelism += worked <= 1 ? 0 : worked;
            }
        }
        if (this.activeCooling && this.hasWork()) {
            this.convertOverflowParallelismToWorkDelay(this.parallelism - this.consumedParallelism);
        }
        this.consumedParallelism = 0;
        if (previous != this.totalCrafted) {
            this.markNoUpdateSync();
        }
    }

    public void supplyCoolantCache() {
        if (this.coolantCache >= MAX_COOLANT_CACHE) {
            return;
        }
        for (final var inputHandler : this.coolantInputHandlers) {
            for (final var property : inputHandler.getTankProperties()) {
                final var contents = property.getContents();
                if (contents == null || contents.amount == 0) {
                    continue;
                }
                final var coolant = MachineCoolants.INSTANCE.getCoolant(contents.getFluid());
                if (coolant == null) {
                    continue;
                }
                for (final var outputHandler : this.coolantOutputHandlers) {
                    final int maximum = coolant.maxCanConsume(inputHandler, outputHandler);
                    if (maximum <= 0) {
                        continue;
                    }
                    final int required = MAX_COOLANT_CACHE - this.coolantCache;
                    int multiple = required / coolant.coolantUnit();
                    if (multiple * coolant.coolantUnit() < required) {
                        multiple++;
                    }
                    multiple = Math.min(multiple, maximum);
                    if (multiple <= 0) {
                        continue;
                    }
                    final var input = coolant.input();
                    inputHandler.drain(new FluidStack(input, multiple * input.amount), true);
                    final var output = coolant.output();
                    if (output != null) {
                        outputHandler.fill(new FluidStack(output, multiple * output.amount), true);
                    }
                    this.coolantCache += multiple * coolant.coolantUnit();
                    if (this.coolantCache >= MAX_COOLANT_CACHE) {
                        return;
                    }
                }
            }
        }
    }

    public void supplyWorkerPower() {
        final IEnergyGrid energy;
        try {
            energy = this.channel.getProxy().getEnergy();
        } catch (final GridAccessException ignored) {
            return;
        }
        for (final var worker : this.getWorkers()) {
            if (worker.getEnergyCache() < worker.getMaxEnergyCache()) {
                worker.supplyEnergy((int) energy.extractAEPower(worker.getMaxEnergyCache() - worker.getEnergyCache(),
                    Actionable.MODULATE, PowerMultiplier.CONFIG));
            }
        }
    }

    public void clearOutputBuffer() {
        try {
            final var proxy = this.channel.getProxy();
            final var inventory = proxy.getStorage().getInventory(this.itemChannel);
            for (final var stack : this.outputBuffer) {
                final var remainder = Platform.poweredInsert(proxy.getEnergy(), inventory, stack.copy(), this.channel.getSource());
                stack.setStackSize(remainder == null ? 0 : remainder.getStackSize());
            }
        } catch (final GridAccessException ignored) {
        }
    }

    @Override
    public void updateComponents() {
        super.updateComponents();
        final var workers = this.getDynamicPattern("workers");
        this.length = workers == null ? 0 : workers.getSize();
        for (final var components : this.foundComponents.values()) {
            for (final var component : components.values()) {
                final var handler = component.providedComponent();
                if (handler instanceof IFluidHandler fluid) {
                    if (component.getComponent().getIOType() == IOType.INPUT) {
                        this.coolantInputHandlers.add(fluid);
                    } else {
                        this.coolantOutputHandlers.add(fluid);
                    }
                }
            }
        }
        this.updateParallelism();
        this.updateWorkDelay();
    }

    @Override
    public void onAddPart(final EFabricatorPart part) {
        if (part instanceof EFabricatorMEChannel channel) {
            this.channel = channel;
        }
    }

    @Override
    public void clearParts() {
        super.clearParts();
        this.coolantInputHandlers.clear();
        this.coolantOutputHandlers.clear();
        this.channel = null;
        this.length = 0;
    }

    public void updateParallelism() {
        double parallelism = 0;
        final List<EFabricatorParallelProc.Modifier> multipliers = new ObjectArrayList<>();
        for (final var processor : this.getParallelProcs()) {
            for (final var modifier : processor.getModifiers()) {
                if (modifier.isBuff() || !this.activeCooling && modifier.debuff()) {
                    if (modifier.type() == EFabricatorParallelProc.Type.MULTIPLY) {
                        multipliers.add(modifier);
                    } else {
                        parallelism = modifier.apply(parallelism);
                    }
                }
            }
            if (this.overclocked) {
                for (final var modifier : processor.getOverclockModifiers()) {
                    if (modifier.isBuff() || !this.activeCooling && modifier.debuff()) {
                        if (modifier.type() == EFabricatorParallelProc.Type.MULTIPLY) {
                            multipliers.add(modifier);
                        } else {
                            parallelism = modifier.apply(parallelism);
                        }
                    }
                }
            }
        }
        for (final var modifier : multipliers) {
            parallelism = modifier.apply(parallelism);
        }
        this.parallelism = roundToInt(parallelism);
    }

    public synchronized void convertOverflowParallelismToWorkDelay(final int overflow) {
        if (overflow <= 0 || this.speedupApplied) {
            return;
        }
        final float ratio = (float) this.parallelism / overflow;
        int speedup = Math.min(roundToInt(ratio / 0.05F), this.maxWorkDelay - 1);
        final double usage = this.parallelism * 0.04;
        speedup = Math.min(speedup, (int) (this.coolantCache / usage));
        final double consumed = speedup * usage;
        if (Double.isNaN(consumed)) {
            throw new IllegalArgumentException("Cannot round NaN value.");
        }
        this.coolantCache -= (int) Math.round(consumed);
        this.workDelay = this.maxWorkDelay - speedup;
        this.speedupApplied = true;
    }

    public void updateWorkDelay() {
        this.maxWorkDelay = this.activeCooling ? WORK_DELAY - this.getWorkers().size() : WORK_DELAY;
    }

    public void recalculateEnergyUsage() {
        double drain = 64.0;
        for (final var bus : this.getPatternBuses()) {
            drain += bus.getValidPatterns();
        }
        if (this.energyConsumePerTick != drain) {
            this.energyConsumePerTick = drain;
            if (this.channel != null) {
                this.channel.getProxy().setIdlePowerUsage(drain);
            }
        }
    }

    public boolean insertPattern(final ItemStack stack) {
        for (final var bus : this.getPatternBuses()) {
            final var inventory = bus.getPatterns();
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    inventory.setStackInSlot(i, ItemUtils.copyStackWithSize(stack, 1));
                    return true;
                }
            }
        }
        return false;
    }

    public boolean offerWork(final CraftWork work) {
        boolean success = false;
        for (final var worker : this.getWorkers()) {
            if (!worker.isFull()) {
                worker.offerWork(work.split(worker.getRemainingSpace()));
                success = true;
                if (work.getSize() < 1) {
                    break;
                }
            }
        }
        if (success && this.activeCooling && !this.speedupApplied) {
            this.convertOverflowParallelismToWorkDelay(this.parallelism);
        }
        return success;
    }

    public boolean isQueueFull() {
        for (final var worker : this.getWorkers()) {
            if (!worker.isFull()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasWork() {
        for (final var worker : this.getWorkers()) {
            if (worker.hasWork()) {
                return true;
            }
        }
        return false;
    }

    public synchronized void updateGUIDataPacket() {
        this.guiDataDirty = true;
    }

    public Levels getLevel() {
        final var block = this.getParentController();
        if (block == BlockEFabricatorController.L4) {
            return Levels.L4;
        }
        if (block == BlockEFabricatorController.L6) {
            return Levels.L6;
        }
        if (block == BlockEFabricatorController.L9) {
            return Levels.L9;
        }
        NovaEngineeringCore.log.warn("Invalid EFabricator controller level: {}", block);
        return Levels.L4;
    }

    public List<EFabricatorWorker> getWorkers() {
        return this.parts.getParts(EFabricatorWorker.class);
    }

    public List<EFabricatorPatternBus> getPatternBuses() {
        return this.parts.getParts(EFabricatorPatternBus.class);
    }

    public List<EFabricatorParallelProc> getParallelProcs() {
        return this.parts.getParts(EFabricatorParallelProc.class);
    }

    public int getAvailableParallelism() {
        return Math.max(0, this.parallelism - this.consumedParallelism);
    }

    public int getEnergyStored() {
        return this.getWorkers().stream().mapToInt(EFabricatorWorker::getEnergyCache).sum();
    }

    public void consumeCoolant(final int amount) {
        this.coolantCache -= amount;
    }

    public int getCoolantInputCap() {
        return capacity(this.coolantInputHandlers);
    }

    public int getCoolantOutputCap() {
        return capacity(this.coolantOutputHandlers);
    }

    public int getCoolantInputFluids() {
        int total = 0;
        for (final var handler : this.coolantInputHandlers) {
            for (final var property : handler.getTankProperties()) {
                final var contents = property.getContents();
                if (contents == null || contents.amount == 0 || MachineCoolants.INSTANCE.getCoolant(contents.getFluid()) == null) {
                    continue;
                }
                total += Math.min(contents.amount, Integer.MAX_VALUE - total);
                if (total == Integer.MAX_VALUE) {
                    return total;
                }
            }
        }
        return total;
    }

    public int getCoolantOutputFluids() {
        int total = 0;
        for (final var handler : this.coolantOutputHandlers) {
            for (final var property : handler.getTankProperties()) {
                final var contents = property.getContents();
                if (contents == null || contents.amount == 0) {
                    continue;
                }
                total += Math.min(contents.amount, Integer.MAX_VALUE - total);
                if (total == Integer.MAX_VALUE) {
                    return total;
                }
            }
        }
        return total;
    }

    @Override
    public Class<? extends Block> getControllerBlock() {
        return BlockEFabricatorController.class;
    }

    private void refreshClientModel() {
        ClientProxy.clientScheduler.addRunnable(() -> {
            BlockModelHider.hideOrShowBlocks(HIDE_POS_LIST, this);
            this.notifyStructureFormedState(this.isStructureFormed());
        }, 0);
    }

    @Override
    public void validate() {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            this.refreshClientModel();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            BlockModelHider.hideOrShowBlocks(HIDE_POS_LIST, this);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            this.refreshClientModel();
        }
    }

    @Override
    public void readCustomNBT(final NBTTagCompound tag) {
        final boolean previouslyLoaded = this.loaded;
        this.loaded = false;
        super.readCustomNBT(tag);
        this.totalCrafted = tag.getLong("totalCrafted");
        this.setOverclocked(tag.getBoolean("overclock"));
        this.setActiveCooling(tag.getBoolean("activeCooling"));
        this.coolantCache = tag.getInteger("coolantCache");
        this.outputBuffer = new ItemList();
        final NBTTagList items = tag.getTagList("outputBuffer", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            this.outputBuffer.add(this.itemChannel.createStack(ItemStackUtils.readNBTOversize(items.getCompoundTagAt(i))));
        }
        this.loaded = previouslyLoaded;
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            this.refreshClientModel();
        }
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        tag.setLong("totalCrafted", this.totalCrafted);
        tag.setBoolean("overclock", this.overclocked);
        tag.setBoolean("activeCooling", this.activeCooling);
        tag.setInteger("coolantCache", this.coolantCache);
        final NBTTagList items = new NBTTagList();
        synchronized (this.outputBuffer) {
            for (final var stack : this.outputBuffer) {
                items.appendTag(ItemStackUtils.writeNBTOversize(stack.getCachedItemStack(stack.getStackSize())));
            }
        }
        tag.setTag("outputBuffer", items);
    }
}
