package github.kasuminova.novaeng.common.tile.ecotech.efabricator

import appeng.api.AEApi
import appeng.api.config.Actionable
import appeng.api.config.PowerMultiplier
import appeng.api.networking.energy.IEnergyGrid
import appeng.api.storage.channels.IItemStorageChannel
import appeng.api.storage.data.IAEItemStack
import appeng.api.storage.data.IItemList
import appeng.me.GridAccessException
import appeng.util.Platform
import appeng.util.item.ItemList
import github.kasuminova.mmce.client.util.ItemStackUtils
import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.client.util.BlockModelHider
import github.kasuminova.novaeng.common.block.ecotech.efabricator.BlockEFabricatorController
import github.kasuminova.novaeng.common.block.ecotech.efabricator.prop.Levels
import github.kasuminova.novaeng.common.network.PktEFabricatorGUIData
import github.kasuminova.novaeng.common.tile.ecotech.EPartController
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorWorker.CraftWork
import github.kasuminova.novaeng.common.util.Functions
import github.kasuminova.novaeng.common.util.MachineCoolants
import hellfirepvp.modularmachinery.ModularMachinery
import hellfirepvp.modularmachinery.client.ClientProxy
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent
import hellfirepvp.modularmachinery.common.machine.IOType
import hellfirepvp.modularmachinery.common.machine.MachineRegistry
import hellfirepvp.modularmachinery.common.util.ItemUtils
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fml.common.FMLCommonHandler
import java.util.function.Consumer
import java.util.function.IntFunction
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.concurrent.Volatile
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Suppress("unused")
class EFabricatorController() : EPartController<EFabricatorPart>() {

    companion object {
        const val MAX_COOLANT_CACHE: Int = 100000
        const val WORK_DELAY: Int = 20

        val HIDE_POS_LIST: MutableList<BlockPos> = Functions.asList( // Center
            BlockPos(0, 1, 0),
            BlockPos(0, -1, 0),

            BlockPos(0, 1, 1),
            BlockPos(0, 0, 1),
            BlockPos(0, -1, 1),  // Left

            BlockPos(1, 1, 0),
            BlockPos(1, 0, 0),
            BlockPos(1, -1, 0),

            BlockPos(1, 1, 1),
            BlockPos(1, 0, 1),
            BlockPos(1, -1, 1),  // Right

            BlockPos(-1, 1, 0),
            BlockPos(-1, 0, 0),
            BlockPos(-1, -1, 0),

            BlockPos(-1, 1, 1),
            BlockPos(-1, 0, 1),
            BlockPos(-1, -1, 1)
        )
    }

    val coolantInputHandlers = ObjectArrayList<IFluidHandler>()
    val coolantOutputHandlers = ObjectArrayList<IFluidHandler>()

    val itemChannel = AEApi.instance().storage()
        .getStorageChannel(IItemStorageChannel::class.java)

    var outputBuffer: IItemList<IAEItemStack> = ItemList()

    var parentController: BlockEFabricatorController? = null
    var energyConsumePerTick: Double = 64.0

    var channel: EFabricatorMEChannel? = null

    var length = 0

    var workDelay = WORK_DELAY
    var maxWorkDelay = WORK_DELAY

    var parallelism = 0
    var consumedParallelism = 0

    var coolantCache = 0

    var totalCrafted: Long = 0

    var speedupApplied = false

    var overclocked = false
        set(value) {
            field = value
            updateParallelism()
            updateGUIDataPacket()
        }

    var activeCooling = false
        set(value) {
            field = value
            updateParallelism()
            updateWorkDelay()
            updateGUIDataPacket()
        }

    var guiDataPacket: PktEFabricatorGUIData? = null
        get() {
            if (guiDataDirty || field == null) {
                field = PktEFabricatorGUIData(this)
                this.guiDataDirty = false
            }
            return field
        }

    @Volatile
    var guiDataDirty = false

    constructor(machineRegistryName: ResourceLocation) : this() {
        this.parentMachine = MachineRegistry.getRegistry().getMachine(machineRegistryName)
        this.parentController = BlockEFabricatorController.REGISTRY[ResourceLocation(
            NovaEngineeringCore.MOD_ID,
            machineRegistryName.getPath()
        )]
    }

    init {
        this.workMode = WorkMode.SEMI_SYNC
    }

    override fun onSyncTick(): Boolean {
        if (channel == null || !channel!!.proxy.isActive) {
            this.tickExecutor = null
            return false
        }

        workDelay--
        if (workDelay > 0) {
            this.tickExecutor = null
            return false
        }
        workDelay = maxWorkDelay
        speedupApplied = false
        clearOutputBuffer()
        supplyWorkerPower()
        supplyCoolantCache()
        return true
    }

    override fun onAsyncTick() {
        updateGUIDataPacket()

        val prevTotalCrafted = totalCrafted
        val workers = this.workers
        workers.forEach(Consumer { worker: EFabricatorWorker? -> worker!!.updateStatus(false) })
        for (worker in workers) {
            if (worker.hasWork()) {
                val worked = worker.doWork()
                totalCrafted += worked.toLong()
                consumedParallelism += if (worked <= 1) 0 else worked
            }
        }
        if (activeCooling && hasWork()) {
            convertOverflowParallelismToWorkDelay(parallelism - consumedParallelism)
        }
        consumedParallelism = 0

        if (prevTotalCrafted != totalCrafted) {
            markNoUpdateSync()
        }
    }

    fun supplyCoolantCache() {
        if (coolantCache >= MAX_COOLANT_CACHE) {
            return
        }

        // 写了一坨大的！
        for (inputHandler in coolantInputHandlers) {
            for (property in inputHandler.getTankProperties()) {
                val contents = property.getContents()
                if (contents == null || contents.amount == 0) {
                    continue
                }

                val coolant = MachineCoolants.INSTANCE.getCoolant(contents.fluid) ?: continue

                for (outputHandler in coolantOutputHandlers) {
                    val maxCanConsume = coolant.maxCanConsume(inputHandler, outputHandler)
                    if (maxCanConsume <= 0) {
                        continue
                    }

                    val required: Int = MAX_COOLANT_CACHE - coolantCache
                    var mul = required / coolant.coolantUnit
                    if (mul * coolant.coolantUnit < required) {
                        mul++
                    }
                    mul = min(mul, maxCanConsume)

                    if (mul > 0) {
                        val input = coolant.input
                        inputHandler.drain(FluidStack(input, mul * input.amount), true)
                        val output = coolant.output
                        if (output != null) {
                            outputHandler.fill(FluidStack(output, mul * output.amount), true)
                        }
                        coolantCache += mul * coolant.coolantUnit
                        if (coolantCache >= MAX_COOLANT_CACHE) {
                            return
                        }
                    }
                }
            }
        }
    }

    fun supplyWorkerPower() {
        val energy: IEnergyGrid
        try {
            energy = channel!!.proxy.energy
        } catch (ignored: GridAccessException) {
            return
        }

        for (worker in this.workers) {
            if (worker.energyCache < worker.getMaxEnergyCache()) {
                worker.supplyEnergy(
                    energy.extractAEPower(
                        (worker.getMaxEnergyCache() - worker.energyCache).toDouble(),
                        Actionable.MODULATE,
                        PowerMultiplier.CONFIG
                    ).toInt()
                )
            }
        }
    }

    fun clearOutputBuffer() {
        try {
            val proxy = this.channel!!.proxy
            val inv = proxy.storage.getInventory(itemChannel)
            for (stack in outputBuffer) {
                val notInserted = Platform.poweredInsert<IAEItemStack?>(
                    proxy.energy,
                    inv,
                    stack.copy(),
                    this.channel!!.source
                )
                if (notInserted != null) {
                    stack.stackSize = notInserted.stackSize
                } else {
                    stack.stackSize = 0
                }
            }
        } catch (ignored: GridAccessException) {
        }
    }

    override fun updateComponents() {
        super.updateComponents()
        val workers = getDynamicPattern("workers")
        this.length = workers?.size ?: 0
        this.foundComponents.values.forEach(Consumer { component: ProcessingComponent<*>? ->
            val handler = component!!.providedComponent()
            if (handler is IFluidHandler) {
                when (component.getComponent().ioType) {
                    IOType.INPUT -> coolantInputHandlers.add(handler)
                    IOType.OUTPUT -> coolantOutputHandlers.add(handler)
                }
            }
        })
        updateParallelism()
        updateWorkDelay()
    }

    override fun onAddPart(part: EFabricatorPart?) {
        if (part is EFabricatorMEChannel) {
            this.channel = part
        }
    }

    override fun clearParts() {
        super.clearParts()
        this.coolantInputHandlers.clear()
        this.coolantOutputHandlers.clear()
        this.channel = null
        this.length = 0
    }

    fun updateParallelism() {
        val parallelism = doubleArrayOf(0.0)
        val modifierMap: MutableMap<EFabricatorParallelProc.Type?, MutableList<EFabricatorParallelProc.Modifier>> =
            this.parallelProcs.stream()
                .flatMap { proc: EFabricatorParallelProc? ->
                    if (overclocked) Stream.concat<EFabricatorParallelProc.Modifier>(
                        proc!!.modifiers.stream(),
                        proc.overclockModifiers.stream()
                    ) else proc!!.modifiers.stream()
                }  // 超频额外添加超频修正器
                .filter { modifier: EFabricatorParallelProc.Modifier -> modifier.isBuff || !activeCooling }  // 主动冷却移除超频的负面效果。
                .collect(
                    Collectors.groupingBy(
                        EFabricatorParallelProc.Modifier::type,
                        {
                            Object2ObjectAVLTreeMap<EFabricatorParallelProc.Type, MutableList<EFabricatorParallelProc.Modifier>>(
                                Comparator.comparingInt<EFabricatorParallelProc.Type>(EFabricatorParallelProc.Type::priority)
                            )
                        },
                        Collectors.toCollection { ObjectArrayList() }
                    )
                )

        modifierMap.values.stream()
            .flatMap { obj: MutableList<EFabricatorParallelProc.Modifier> -> obj.stream() }
            .filter { obj: EFabricatorParallelProc.Modifier -> obj.isBuff }
            .forEach { modifier: EFabricatorParallelProc.Modifier ->
                parallelism[0] = modifier.apply(parallelism[0])
            }
        modifierMap.values.stream()
            .flatMap { obj: MutableList<EFabricatorParallelProc.Modifier> -> obj.stream() }
            .filter(EFabricatorParallelProc.Modifier::debuff)
            .forEach { modifier: EFabricatorParallelProc.Modifier ->
                parallelism[0] = modifier.apply(parallelism[0])
            }

        this.parallelism = parallelism[0].roundToLong().toInt()
    }

    @Synchronized
    fun convertOverflowParallelismToWorkDelay(overflow: Int) {
        if (overflow <= 0 || speedupApplied) {
            return
        }
        val ratio = parallelism.toFloat() / overflow
        var speedUp = min((ratio / 0.05f).roundToInt(), maxWorkDelay - 1)

        val coolantUsage = parallelism * 0.04
        val maxCanConsume = (coolantCache / coolantUsage).toInt()
        speedUp = min(speedUp, maxCanConsume)
        coolantCache -= (speedUp * coolantUsage).roundToLong().toInt()

        this.workDelay = maxWorkDelay - speedUp
        this.speedupApplied = true
    }

    fun updateWorkDelay() {
        if (activeCooling) {
            this.maxWorkDelay = WORK_DELAY - this.workers.size
        } else {
            this.maxWorkDelay = WORK_DELAY
        }
    }

    fun recalculateEnergyUsage() {
        var newIdleDrain = 64.0
        val allPatterns =
            this.patternBuses.stream().mapToInt { obj: EFabricatorPatternBus? -> obj!!.validPatterns }.sum()
        newIdleDrain += allPatterns.toDouble()
        if (this.energyConsumePerTick != newIdleDrain) {
            this.energyConsumePerTick = newIdleDrain
            if (this.channel != null) {
                this.channel!!.proxy.idlePowerUsage = this.energyConsumePerTick
            }
        }
    }

    fun insertPattern(patternStack: ItemStack): Boolean {
        for (patternBus in this.patternBuses) {
            val patternInv = patternBus.patterns
            for (i in 0..<patternInv.slots) {
                if (patternInv.getStackInSlot(i).isEmpty) {
                    patternInv.setStackInSlot(i, ItemUtils.copyStackWithSize(patternStack, 1))
                    return true
                }
            }
        }

        return false
    }

    fun offerWork(work: CraftWork): Boolean {
        var success = false
        for (worker in this.workers) {
            if (!worker.isFull) {
                val i = worker.remainingSpace
                worker.offerWork(work.split(i))
                success = true
                if (work.size < 1) {
                    break
                }
            }
        }
        if (success && activeCooling && !speedupApplied) {
            convertOverflowParallelismToWorkDelay(parallelism)
        }
        return success
    }

    val isQueueFull: Boolean
        get() {
            for (worker in this.workers) {
                if (!worker.isFull) {
                    return false
                }
            }
            return true
        }

    fun hasWork(): Boolean {
        for (eFabricatorWorker in this.workers) {
            if (eFabricatorWorker.hasWork()) {
                return true
            }
        }
        return false
    }

    @Synchronized
    fun updateGUIDataPacket() {
        guiDataDirty = true
    }

    val level: Levels
        get() {
            if (parentController === BlockEFabricatorController.L4) {
                return Levels.L4
            }
            if (parentController === BlockEFabricatorController.L6) {
                return Levels.L6
            }
            if (parentController === BlockEFabricatorController.L9) {
                return Levels.L9
            }
            NovaEngineeringCore.log.warn("Invalid EFabricator controller level: {}", parentController)
            return Levels.L4
        }

    val workers: List<EFabricatorWorker>
        get() = parts.getParts(EFabricatorWorker::class.java)

    val patternBuses: List<EFabricatorPatternBus>
        get() = parts.getParts(EFabricatorPatternBus::class.java)

    val parallelProcs: List<EFabricatorParallelProc>
        get() = parts.getParts(EFabricatorParallelProc::class.java)

    val availableParallelism
        get() = max(0, parallelism - consumedParallelism)

    val energyStored: Int
        get() = this.workers.stream().mapToInt { obj: EFabricatorWorker -> obj.energyCache }.sum()

    fun consumeCoolant(amount: Int) {
        coolantCache -= amount
    }

    val coolantInputCap: Int
        get() {
            var total = 0
            for (handler in coolantInputHandlers) {
                for (property in handler.getTankProperties()) {
                    total += min(property.getCapacity(), Int.MAX_VALUE - total)
                    if (total == Int.MAX_VALUE) {
                        return Int.MAX_VALUE
                    }
                }
            }
            return total
        }

    val coolantInputFluids: Int
        get() {
            var total = 0
            for (handler in coolantInputHandlers) {
                for (property in handler.getTankProperties()) {
                    val contents = property.getContents()
                    if (contents == null || contents.amount == 0) {
                        continue
                    }
                    if (MachineCoolants.INSTANCE.getCoolant(contents.fluid) != null) {
                        total += min(contents.amount, Int.MAX_VALUE - total)
                        if (total >= Int.MAX_VALUE) {
                            return Int.MAX_VALUE
                        }
                    }
                }
            }
            return total
        }

    val coolantOutputCap: Int
        get() {
            var total = 0
            for (handler in coolantOutputHandlers) {
                for (property in handler.getTankProperties()) {
                    total += min(property.getCapacity(), Int.MAX_VALUE - total)
                    if (total == Int.MAX_VALUE) {
                        return Int.MAX_VALUE
                    }
                }
            }
            return total
        }

    val coolantOutputFluids: Int
        get() {
            var total = 0
            for (handler in coolantOutputHandlers) {
                for (property in handler.getTankProperties()) {
                    val contents = property.getContents()
                    if (contents == null || contents.amount == 0) {
                        continue
                    }
                    total += min(contents.amount, Int.MAX_VALUE - total)
                    if (total == Int.MAX_VALUE) {
                        return Int.MAX_VALUE
                    }
                }
            }
            return total
        }

    override fun getControllerBlock(): Class<out Block?> {
        return BlockEFabricatorController::class.java
    }

    override fun validate() {
        if (!FMLCommonHandler.instance().getEffectiveSide().isClient) {
            return
        }

        ClientProxy.clientScheduler.addRunnable({
            BlockModelHider.hideOrShowBlocks(HIDE_POS_LIST, this)
            notifyStructureFormedState(isStructureFormed)
        }, 0)
    }

    override fun invalidate() {
        super.invalidate()
        if (FMLCommonHandler.instance().getEffectiveSide().isClient) {
            BlockModelHider.hideOrShowBlocks(HIDE_POS_LIST, this)
        }
    }

    override fun onLoad() {
        super.onLoad()
        if (!FMLCommonHandler.instance().getEffectiveSide().isClient) {
            return
        }
        ClientProxy.clientScheduler.addRunnable({
            BlockModelHider.hideOrShowBlocks(HIDE_POS_LIST, this)
            notifyStructureFormedState(isStructureFormed)
        }, 0)
    }

    override fun readCustomNBT(compound: NBTTagCompound) {
        val prevLoaded = loaded
        loaded = false

        super.readCustomNBT(compound)
        totalCrafted = compound.getLong("totalCrafted")
        overclocked = compound.getBoolean("overclock")
        activeCooling = compound.getBoolean("activeCooling")
        coolantCache = compound.getInteger("coolantCache")

        outputBuffer = ItemList()
        val list = compound.getTagList("outputBuffer", Constants.NBT.TAG_COMPOUND)
        IntStream.range(0, list.tagCount())
            .mapToObj<IAEItemStack?>(IntFunction { i: Int ->
                itemChannel.createStack(
                    ItemStackUtils.readNBTOversize(
                        list.getCompoundTagAt(
                            i
                        )
                    )
                )
            })
            .forEach { t: IAEItemStack? -> outputBuffer.add(t) }

        loaded = prevLoaded

        if (FMLCommonHandler.instance().getEffectiveSide().isClient) {
            ClientProxy.clientScheduler.addRunnable({
                BlockModelHider.hideOrShowBlocks(HIDE_POS_LIST, this)
                notifyStructureFormedState(isStructureFormed)
            }, 0)
        }
    }

    override fun writeCustomNBT(compound: NBTTagCompound) {
        super.writeCustomNBT(compound)
        compound.setLong("totalCrafted", totalCrafted)
        compound.setBoolean("overclock", overclocked)
        compound.setBoolean("activeCooling", activeCooling)
        compound.setInteger("coolantCache", coolantCache)

        val list = NBTTagList()
        synchronized(outputBuffer) {
            for (stack in outputBuffer) {
                list.appendTag(ItemStackUtils.writeNBTOversize(stack.getCachedItemStack(stack.stackSize)))
            }
        }
        compound.setTag("outputBuffer", list)
    }

    override fun readMachineNBT(compound: NBTTagCompound) {
        super.readMachineNBT(compound)
        if (compound.hasKey("parentMachine")) {
            val rl = ResourceLocation(compound.getString("parentMachine"))
            parentMachine = MachineRegistry.getRegistry().getMachine(rl)
            if (parentMachine != null) {
                this.parentController = BlockEFabricatorController.REGISTRY[ResourceLocation(
                    NovaEngineeringCore.MOD_ID,
                    parentMachine.getRegistryName().getPath()
                )]
            } else {
                ModularMachinery.log.info("Couldn't find machine named " + rl + " for controller at " + getPos())
            }
        }
    }
}