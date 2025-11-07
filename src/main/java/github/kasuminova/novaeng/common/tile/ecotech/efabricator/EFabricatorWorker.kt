package github.kasuminova.novaeng.common.tile.ecotech.efabricator

import appeng.api.config.Actionable
import appeng.api.networking.energy.IEnergyGrid
import appeng.api.storage.data.IAEItemStack
import appeng.api.storage.data.IItemList
import appeng.me.GridAccessException
import appeng.util.item.AEItemStack
import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.common.block.ecotech.efabricator.prop.WorkerStatus
import github.kasuminova.novaeng.common.network.PktEFabricatorWorkerStatusUpdate
import hellfirepvp.modularmachinery.common.util.ItemUtils
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.network.NetworkRegistry
import java.util.ArrayDeque
import java.util.Arrays
import java.util.Deque
import kotlin.math.max
import kotlin.math.min

open class EFabricatorWorker : EFabricatorPart() {

    companion object {
        const val MAX_ENERGY_CACHE: Int = 500000
        const val MAX_QUEUE_DEPTH: Int = 32

        const val ENERGY_USAGE: Int = 100
        const val COOLANT_USAGE: Int = 5
    }

    val queue = CraftingQueue()

    var status = WorkerStatus.OFF
        set(value) {
            field = value
            if (FMLCommonHandler.instance().getEffectiveSide().isServer) {
                markNoUpdateSync()
            }
        }
    var queueDepth = MAX_QUEUE_DEPTH
        get() {
            val controller = getController()
            if (controller != null && controller.overclocked) {
                return controller.level.applyOverclockQueueDepth(field)
            }
            return field
        }

    var energyCache = 0

    var updateTick = 0L

    val remainingSpace: Int
        get() = this.queueDepth - this.queue.size()

    fun getMaxEnergyCache(): Int {
        return MAX_ENERGY_CACHE
    }

    @Synchronized
    fun doWork(): Int {
        val controller = partController
        val coolantCache = controller.coolantCache
        val energyUsage: Int = if (controller.overclocked && !controller.activeCooling) {
            controller.level.applyOverclockEnergyUsage(ENERGY_USAGE)
        } else {
            ENERGY_USAGE
        }
        var energy = this.energyCache.toDouble()
        val c = controller.channel
        var grid: IEnergyGrid? = null
        if (c != null) {
            try {
                grid = c.proxy.grid.getCache(IEnergyGrid::class.java)
                energy += grid.storedPower
            } catch (ignored: GridAccessException) {
            }
        }
        var parallelism = min(max(controller.availableParallelism, 1).toDouble(), energy / energyUsage).toInt()
        if (controller.activeCooling) {
            parallelism = min(parallelism, coolantCache / COOLANT_USAGE)
        }

        var completed = 0
        val outputBuffer: IItemList<IAEItemStack> = controller.outputBuffer
        var craftWork: CraftWork
        synchronized(outputBuffer) {
            while ((parallelism > completed) && !queue.isEmpty) {
                craftWork = queue.poll()
                val workSize = parallelism - completed
                val size = craftWork.size
                val out: CraftWork
                if (size > workSize) {
                    out = craftWork.split(workSize)
                    queue.add(craftWork)
                } else {
                    out = craftWork
                }
                for (remain in out.remaining) {
                    if (!remain.isEmpty) {
                        outputBuffer.add(AEItemStack.fromItemStack(remain))
                    }
                }
                outputBuffer.add(AEItemStack.fromItemStack(out.output))

                completed += out.size
            }
        }

        if (completed > 0) {
            var `in` = energyUsage * completed
            if (`in` > energyCache) {
                `in` -= energyCache
                energyCache = 0
                if (grid != null) {
                    synchronized(grid) {
                        grid.injectPower(`in`.toDouble(), Actionable.MODULATE)
                    }
                }
            } else {
                energyCache -= `in`
            }
            if (controller.activeCooling) {
                controller.consumeCoolant(COOLANT_USAGE * completed)
            }
        }
        return completed
    }

    fun supplyEnergy(energy: Int) {
        energyCache += energy
    }

    fun offerWork(craftWork: CraftWork) {
        queue.add(craftWork)
    }

    fun hasWork(): Boolean {
        return !queue.isEmpty
    }

    val isFull: Boolean
        get() = queue.size() >= queueDepth

    override fun onAssembled() {
        updateStatus(true)
        super.onAssembled()
    }

    override fun onDisassembled() {
        updateStatus(true)
        super.onDisassembled()
    }

    fun updateStatus(force: Boolean) {
        val prevUpdateTick = updateTick
        val updateTick = getWorld().totalWorldTime

        if (!force && status == WorkerStatus.RUN && prevUpdateTick + 20 >= updateTick) {
            if (hasWork()) {
                this@EFabricatorWorker.updateTick = updateTick
            }
            return
        }

        if (controller == null) {
            if (status != WorkerStatus.OFF) {
                status = WorkerStatus.OFF
            }
        } else if (hasWork()) {
            if (status != WorkerStatus.RUN) {
                status = WorkerStatus.RUN
            }
        } else {
            if (status != WorkerStatus.ON) {
                status = WorkerStatus.ON
            }
        }
        this@EFabricatorWorker.updateTick = updateTick
    }

    override fun markNoUpdate() {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer) {
            NovaEngineeringCore.NET_CHANNEL.sendToAllTracking(
                PktEFabricatorWorkerStatusUpdate(getPos(), status),
                NetworkRegistry.TargetPoint(
                    world.provider.dimension,
                    pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                    -1.0
                )
            )
        }
        super.markNoUpdate()
    }

    override fun readCustomNBT(compound: NBTTagCompound) {
        queue.readFromNBT(compound)
        energyCache = compound.getInteger("energyCache")
        status = WorkerStatus.entries[compound.getByte("status").toInt()]
        super.readCustomNBT(compound)
    }

    override fun writeCustomNBT(compound: NBTTagCompound) {
        queue.writeToNBT(compound)
        compound.setInteger("energyCache", energyCache)
        compound.setByte("status", status.ordinal.toByte())
        super.writeCustomNBT(compound)
    }

    class CraftingQueue {
        val queue: Deque<CraftWork> = ArrayDeque<CraftWork>()
        private var size = 0

        fun size(): Int {
            return size
        }

        val isEmpty: Boolean
            get() = queue.isEmpty()

        fun add(craftWork: CraftWork) {
            queue.add(craftWork)
            size += craftWork.size
        }

        fun poll(): CraftWork {
            val i = queue.poll()
            if (i != null) {
                size -= i.size
            } else {
                size = 0
            }
            return i
        }

        fun peek(): CraftWork? {
            return queue.peek()
        }

        fun writeToNBT(nbt: NBTTagCompound): NBTTagCompound {
            if (queue.isEmpty()) {
                return nbt
            }

            val stackSet = ObjectArrayList<ItemStack>()

            // Queue
            val queueTag = NBTTagList()
            var prev: CraftWork? = null
            var repeat = 0
            for (craftWork in queue) {
                if (prev != null && prev == craftWork) {
                    repeat++
                    continue
                }
                if (repeat > 0) {
                    queueTag.getCompoundTagAt(queueTag.tagCount() - 1).setShort(REPEAT_TAG, repeat.toShort())
                    repeat = 0
                }
                queueTag.appendTag(craftWork.writeToNBT(stackSet))
                prev = craftWork
            }
            if (repeat > 0) {
                queueTag.getCompoundTagAt(queueTag.tagCount() - 1).setShort(REPEAT_TAG, repeat.toShort())
            }
            nbt.setTag(QUEUE_TAG, queueTag)

            // StackSet
            val stackSetTag = NBTTagCompound()
            for (i in stackSet.indices) {
                val stack = stackSet[i]
                if (!stack.isEmpty) {
                    stackSetTag.setTag(STACK_SET_TAG_ID_PREFIX + i, stack.serializeNBT())
                }
            }
            nbt.setTag(STACK_SET_TAG, stackSetTag)
            nbt.setInteger(STACK_SET_SIZE_TAG, stackSet.size)

            return nbt
        }

        fun readFromNBT(nbt: NBTTagCompound) {
            queue.clear()
            val stackSet = ObjectArrayList<ItemStack>()

            // StackSet
            val stackSetTag = nbt.getCompoundTag(STACK_SET_TAG)
            for (i in 0..<nbt.getInteger(STACK_SET_SIZE_TAG)) {
                stackSet.add(ItemStack(stackSetTag.getCompoundTag(STACK_SET_TAG_ID_PREFIX + i)))
            }

            // Queue
            val queueTag = nbt.getTagList(QUEUE_TAG, Constants.NBT.TAG_COMPOUND)
            for (i in 0..<queueTag.tagCount()) {
                val tagAt = queueTag.getCompoundTagAt(i)
                val work = CraftWork(tagAt, stackSet)
                queue.add(work)
                val repeat = tagAt.getShort(REPEAT_TAG)
                for (r in 0..<repeat) {
                    queue.add(work.copy())
                }
            }
        }

        companion object {
            private const val QUEUE_TAG = "Q"
            private const val STACK_SET_TAG = "SS"
            private const val STACK_SET_TAG_ID_PREFIX = "S#"
            private const val STACK_SET_SIZE_TAG = "SSS"
            private const val REPEAT_TAG = "R"
        }
    }

    class CraftWork {
        internal val remaining: Array<ItemStack>
        val output: ItemStack
        internal var size: Int

        constructor(remaining: Array<ItemStack>, output: ItemStack, size: Int) {
            this.remaining = remaining
            this.output = output
            this.size = max(1, size)
        }

        constructor(nbt: NBTTagCompound, stackSet: MutableList<ItemStack>) {
            remaining = Array<ItemStack>(
                nbt.getByte(REMAIN_SIZE_TAG).toInt()
            ) { i -> ItemStack.EMPTY }
            for (remainIdx in remaining.indices) {
                val setIdx =
                    if (nbt.hasKey(REMAIN_TAG_PREFIX + remainIdx)) nbt.getInteger(REMAIN_TAG_PREFIX + remainIdx) else -1
                remaining[remainIdx] = if (setIdx == -1) ItemStack.EMPTY else stackSet[setIdx]
            }
            output = stackSet[nbt.getInteger(OUTPUT_TAG)]
            size = max(1, nbt.getInteger(SIZE))
        }

        fun split(amount: Int): CraftWork {
            val i = min(amount, this.size)
            if (i > 0) {
                val inputs: Array<ItemStack> = Array<ItemStack>(
                    this.remaining.size
                ) { i -> ItemStack.EMPTY }
                for (ii in remaining.indices) {
                    inputs[ii] = remaining[ii].splitStack(i)
                }
                val output = this.output.copy()
                val eachOutput = this.output.count / size
                val outCount = i * eachOutput
                output.setCount(outCount)
                this.output.shrink(outCount)
                size -= i
                return CraftWork(inputs, output, i)
            } else {
                val inputs = Array<ItemStack>(this.remaining.size) { i -> ItemStack.EMPTY }
                return CraftWork(inputs, ItemStack.EMPTY, 0)
            }
        }

        fun writeToNBT(stackSet: MutableList<ItemStack>): NBTTagCompound {
            val nbt = NBTTagCompound()

            // Input.
            nbt.setByte(REMAIN_SIZE_TAG, remaining.size.toByte())
            remain@ for (remainIdx in remaining.indices) {
                val remain = remaining[remainIdx]
                if (remain.isEmpty) {
                    continue
                }

                for (setIdx in stackSet.indices) {
                    if (matchStacksStrict(remain, stackSet[setIdx])) {
                        nbt.setShort(REMAIN_TAG_PREFIX + remainIdx, setIdx.toShort())
                        continue@remain
                    }
                }

                stackSet.add(remain)
                nbt.setShort(REMAIN_TAG_PREFIX + remainIdx, (stackSet.size - 1).toShort())
            }

            // Output
            for (setIdx in stackSet.indices) {
                if (matchStacksStrict(output, stackSet[setIdx])) {
                    nbt.setShort(OUTPUT_TAG, setIdx.toShort())
                    return nbt
                }
            }

            stackSet.add(output)
            nbt.setShort(OUTPUT_TAG, (stackSet.size - 1).toShort())
            nbt.setInteger(SIZE, size)
            return nbt
        }

        fun copy(): CraftWork {
            val remaining = Arrays.stream<ItemStack>(this.remaining)
                .map { obj: ItemStack -> obj.copy() }
                .toArray {
                    Array<ItemStack>(
                        0
                    ) { ItemStack.EMPTY }
                }
            return CraftWork(remaining, output.copy(), this.size)
        }

        override fun equals(other: Any?): Boolean {
            if (other is CraftWork) {
                for (i in remaining.indices) {
                    if (!matchStacksStrict(remaining[i], other.remaining[i])) {
                        return false
                    }
                }
                return matchStacksStrict(output, other.output)
            }
            return false
        }

        companion object {
            private const val REMAIN_TAG_PREFIX = "R#"
            private val REMAIN_SIZE_TAG = REMAIN_TAG_PREFIX + "S"
            private const val OUTPUT_TAG = "O"
            private const val SIZE = "Z"

            private fun matchStacksStrict(stack1: ItemStack, stack2: ItemStack): Boolean {
                return ItemUtils.matchStacks(stack1, stack2) && stack1.count == stack2.count
            }
        }

        override fun hashCode(): Int {
            var result = size
            result = 31 * result + remaining.contentHashCode()
            result = 31 * result + output.hashCode()
            return result
        }
    }
}