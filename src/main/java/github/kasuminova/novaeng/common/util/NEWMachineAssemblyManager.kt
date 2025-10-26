package github.kasuminova.novaeng.common.util

import appeng.api.AEApi
import appeng.api.config.Actionable
import appeng.api.networking.storage.IStorageGrid
import appeng.api.storage.IMEMonitor
import appeng.api.storage.channels.IFluidStorageChannel
import appeng.api.storage.channels.IItemStorageChannel
import appeng.api.storage.data.IAEFluidStack
import appeng.api.storage.data.IAEItemStack
import appeng.api.storage.data.IAEStack
import appeng.fluids.util.AEFluidStack
import appeng.helpers.WirelessTerminalGuiObject
import appeng.me.helpers.PlayerSource
import appeng.util.item.AEItemStack
import com.circulation.random_complement.common.handler.MEHandler
import hellfirepvp.modularmachinery.common.util.BlockArray
import hellfirepvp.modularmachinery.common.util.MiscUtils
import ink.ikx.mmce.common.assembly.MachineAssembly
import ink.ikx.mmce.common.utils.FluidUtils
import ink.ikx.mmce.common.utils.StackUtils
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.Tuple
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.world.World
import net.minecraftforge.common.util.BlockSnapshot
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.fluids.BlockFluidBase
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidUtil
import net.minecraftforge.fluids.UniversalBucket
import net.minecraftforge.fluids.capability.IFluidHandlerItem
import java.util.*
import java.util.function.Function

class NEWMachineAssemblyManager {

    companion object {
        private val ADDITIONAL_CONSTRUCTORS =
            Reference2ObjectOpenHashMap<Class<out TileEntity>, EnumMap<EnumFacing, AssemblyBlockArray>>()
        private val MACHINE_ASSEMBLY_CACHE =
            Object2ObjectOpenHashMap<EntityPlayer, AssemblyBlockArray>()

        fun getConstructorsIterator(): ObjectIterator<Map.Entry<Class<out TileEntity>, EnumMap<EnumFacing, AssemblyBlockArray>>> {
            return ADDITIONAL_CONSTRUCTORS.entries.iterator()
        }

        fun addAssemblyMachine(player: EntityPlayer?, array: BlockArray): AssemblyBlockArray {
            val newArray = AssemblyBlockArray(array)
            MACHINE_ASSEMBLY_CACHE[player] = newArray
            return newArray
        }

        fun getMachineAssembly(player: EntityPlayer?): AssemblyBlockArray? {
            return MACHINE_ASSEMBLY_CACHE[player]
        }

        fun checkMachineAssembly(player: EntityPlayer?): Boolean {
            return MACHINE_ASSEMBLY_CACHE.containsKey(player)
        }

        fun removeMachineAssembly(player: EntityPlayer?) {
            val r: AssemblyBlockArray? = MACHINE_ASSEMBLY_CACHE.remove(player)
            r?.end()
        }

        /**
         * MachineAssembly#getFluidHandlerItems(List)
         */
        private fun getFluidHandlerItems(inventory: MutableList<ItemStack>): List<IFluidHandlerItem?> {
            val fluidHandlers: MutableList<IFluidHandlerItem?> = ObjectArrayList<IFluidHandlerItem?>()
            for (invStack in inventory) {
                val item = invStack.item
                // TODO Bucket are not supported at this time.
                if (item is UniversalBucket || item === Items.LAVA_BUCKET || item === Items.WATER_BUCKET) {
                    continue
                }
                if (!FluidUtils.isFluidHandler(invStack)) {
                    continue
                }
                val fluidHandler = FluidUtil.getFluidHandler(invStack)
                if (fluidHandler != null) {
                    fluidHandlers.add(fluidHandler)
                }
            }
            return fluidHandlers
        }
    }

    enum class OperatingStatus {
        SUCCESS,
        ALREADY_EXISTS,
        FAILURE,
        COMPLETE
    }

    class AssemblyBlockArray : BlockArray {

        companion object {
            private val material =
                Object2ReferenceOpenHashMap<BlockInformation, ObjectArrayList<Tuple<Ingredient, IBlockState>>>()
        }

        var usingAE = true
        var ignoreFluids = true
        private var queue: Queue<BlockPos>? = null

        constructor(uid: Long) : super(uid)

        constructor(other: BlockArray) : super(other)

        constructor(other: BlockArray, offset: BlockPos) : super(other, offset)

        fun copy(): AssemblyBlockArray {
            return AssemblyBlockArray(this)
        }

        fun offset(offset: BlockPos): AssemblyBlockArray {
            return AssemblyBlockArray(this, offset)
        }

        fun end() {
            this.pattern.clear()
            queue = null
        }

        fun start(usingAE: Boolean = true, ignoreFluids: Boolean = true) {
            queue = ArrayDeque()
            val l = Function { b: BlockInformation -> ObjectArrayList<Tuple<Ingredient, IBlockState>>() }
            for (entry in this.pattern.entries) {
                queue!!.add(entry.key)
                val info = entry.value
                if (material.containsKey(info)) continue
                for (stateDescriptor in info.matchingStates) {
                    for (state in stateDescriptor.applicable) {
                        val block = state.getBlock()
                        val ingredient = if (block is BlockFluidBase) {
                            Ingredient(
                                FluidStack(block.fluid, 1000)
                            )
                        } else if (block is BlockLiquid) {
                            val material1 = state.getMaterial()
                            if (material1 === Material.LAVA) {
                                Ingredient(FluidStack(FluidRegistry.LAVA, 1000))
                            } else Ingredient(FluidStack(FluidRegistry.WATER, 1000))
                        } else {
                            Ingredient(StackUtils.getStackFromBlockState(state))
                        }
                        material.computeIfAbsent(info, l).add(Tuple<Ingredient, IBlockState>(ingredient, state))
                    }
                }
            }
        }

        fun isFluid(state: IBlockState): Boolean {
            val block = state.getBlock()
            return block is BlockLiquid || block is BlockFluidBase
        }

        fun assemblyBlock(world: World, player: EntityPlayerMP): OperatingStatus {
            val info: BlockInformation?
            val pos = queue!!.poll() ?: return OperatingStatus.COMPLETE
            synchronized(pattern) {
                info = this.pattern.remove(pos)
            }
            if (info == null) {
                return OperatingStatus.ALREADY_EXISTS
            }
            if (player.isCreative) {
                player.getServer()!!.addScheduledTask { world.setBlockState(pos, info.sampleState, 1 or 2 or 16) }
                return OperatingStatus.SUCCESS
            }
            val oldState = world.getBlockState(pos)
            if (oldState != null &&
                (oldState.getBlock() !== Blocks.AIR
                        && !(ignoreFluids && isFluid(oldState)))
            ) {
                return if (matchesState(info, oldState)) {
                    OperatingStatus.ALREADY_EXISTS
                } else {
                    val posToString = MiscUtils.posToString(pos)
                    player.sendMessage(
                        TextComponentTranslation(
                            "message.assembly.tip.cannot_replace",
                            posToString
                        )
                    )
                    OperatingStatus.FAILURE
                }
            }

            val list = material[info] ?: throw RuntimeException("Unknown BlockInformation")

            var hasAE = false
            var wobj: WirelessTerminalGuiObject? = null
            var items: IMEMonitor<IAEItemStack>? = null
            var fluids: IMEMonitor<IAEFluidStack>? = null
            if (usingAE) {
                wobj = MEHandler.getTerminalGuiObject(player)
                val node = wobj.getActionableNode()
                val grid = node.grid.getCache<IStorageGrid>(IStorageGrid::class.java)
                items = grid.getInventory(
                    AEApi.instance().storage()
                        .getStorageChannel(IItemStorageChannel::class.java)
                )
                fluids = grid.getInventory(
                    AEApi.instance().storage()
                        .getStorageChannel(IFluidStorageChannel::class.java)
                )
                hasAE = true
            }
            val itemInventory = player.inventory.mainInventory
            val fluidInventory = getFluidHandlerItems(itemInventory)
            for (ingredientAndIBlockState in list) {
                val ingredient = ingredientAndIBlockState.first
                if (ingredient.isItem) {
                    if (MachineAssembly.consumeInventoryItem(
                            ingredient.itemStack,
                            itemInventory
                        )
                    ) {
                        placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                        return OperatingStatus.SUCCESS
                    } else if (hasAE) {
                        val item = items!!.extractItems(
                            ingredient.aEItemStack,
                            Actionable.MODULATE,
                            PlayerSource(player, wobj)
                        )
                        if (item == null || item.stackSize == 0L) continue
                        placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                        return OperatingStatus.SUCCESS
                    }
                } else {
                    if (MachineAssembly.consumeInventoryFluid(
                            ingredient.fluidStack,
                            fluidInventory
                        )
                    ) {
                        placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                        return OperatingStatus.SUCCESS
                    } else if (hasAE) {
                        val fluid = fluids!!.extractItems(
                            ingredient.aEFluidStack,
                            Actionable.SIMULATE,
                            PlayerSource(player, wobj)
                        )
                        if (fluid == null || fluid.stackSize < 1000) continue
                        fluids.extractItems(
                            ingredient.aEFluidStack,
                            Actionable.MODULATE,
                            PlayerSource(player, wobj)
                        )
                        placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                        return OperatingStatus.SUCCESS
                    }
                }
            }
            val posToString = MiscUtils.posToString(pos)
            player.sendMessage(
                TextComponentTranslation(
                    "message.assembly.tip.missing",
                    posToString
                )
            )
            return OperatingStatus.FAILURE
        }

        private fun placeBlock(player: EntityPlayerMP, world: World, pos: BlockPos, state: IBlockState) {
            player.getServer()!!.addScheduledTask {
                if (ForgeEventFactory.onBlockPlace(player, BlockSnapshot(world, pos, state), EnumFacing.UP)
                        .isCanceled
                ) {
                    player.inventory.placeItemBackInInventory(world, StackUtils.getStackFromBlockState(state))
                } else {
                    world.setBlockState(pos, state, 1 or 2 or 16)
                }
            }
        }

        fun matchesState(info: BlockInformation, state: IBlockState): Boolean {
            val atBlock = state.getBlock()
            val atMeta = atBlock.getMetaFromState(state)

            for (descriptor in info.matchingStates) {
                for (applicable in descriptor.applicable) {
                    val type = applicable.getBlock()
                    val meta = type.getMetaFromState(applicable)
                    if (type == atBlock && meta == atMeta) {
                        return true
                    }
                }
            }
            return false
        }
    }

    class Ingredient {
        val ingredient: Any
        val isItem: Boolean
        val aeStack: IAEStack<*>?

        constructor(stack: ItemStack) {
            this.ingredient = stack
            this.isItem = true
            this.aeStack = AEItemStack.fromItemStack(stack)
        }

        constructor(stack: FluidStack) {
            this.ingredient = stack
            this.isItem = false
            this.aeStack = AEFluidStack.fromFluidStack(stack)
        }

        override fun equals(other: Any?): Boolean {
            if (other == null || javaClass != other.javaClass) return false
            val that = other as Ingredient
            return isItem == that.isItem && aeStack == that.aeStack
        }

        override fun hashCode(): Int {
            return aeStack.hashCode()
        }

        val itemStack: ItemStack?
            get() = ingredient as ItemStack?

        val aEItemStack: IAEItemStack?
            get() = aeStack as IAEItemStack?

        val fluidStack: FluidStack?
            get() = ingredient as FluidStack?

        val aEFluidStack: IAEFluidStack?
            get() = aeStack as IAEFluidStack?
    }
}