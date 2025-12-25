package github.kasuminova.novaeng.common.util

import appeng.api.AEApi
import appeng.api.config.Actionable
import appeng.api.networking.storage.IStorageGrid
import appeng.api.storage.IMEMonitor
import appeng.api.storage.channels.IFluidStorageChannel
import appeng.api.storage.channels.IItemStorageChannel
import appeng.api.storage.data.IAEFluidStack
import appeng.api.storage.data.IAEItemStack
import appeng.helpers.WirelessTerminalGuiObject
import appeng.me.helpers.PlayerSource
import com.circulation.random_complement.common.util.MEHandler
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager.Ingredient
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager.OperatingStatus
import hellfirepvp.modularmachinery.common.util.BlockArray
import hellfirepvp.modularmachinery.common.util.MiscUtils
import ink.ikx.mmce.common.assembly.MachineAssembly
import ink.ikx.mmce.common.utils.FluidUtils
import ink.ikx.mmce.common.utils.StackUtils
import ink.ikx.mmce.common.utils.StructureIngredient
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
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
import net.minecraftforge.fluids.capability.IFluidHandlerItem
import java.util.ArrayDeque
import java.util.Queue
import java.util.function.Function

class AssemblyBlockArray : BlockArray {

    private data class FluidInventory(val slot: Int, val fluid: IFluidHandlerItem)

    companion object {
        private val material =
            Object2ReferenceOpenHashMap<BlockInformation, ObjectArrayList<Tuple<Ingredient, IBlockState>>>()

        /**
         * MachineAssembly#getFluidHandlerItems(List)
         */
        private fun getFluidHandlerItems(inventory: List<ItemStack>): List<FluidInventory> {
            val fluidHandlers = ObjectArrayList<FluidInventory>()
            for ((index, invStack) in inventory.withIndex()) {
                if (!FluidUtils.isFluidHandler(invStack)) {
                    continue
                }
                val fluidHandler = FluidUtil.getFluidHandler(invStack)
                if (fluidHandler != null) {
                    fluidHandlers.add(FluidInventory(index, fluidHandler))
                }
            }
            return fluidHandlers
        }

        private fun consumeInventoryFluid(
            required: FluidStack,
            fluidHandlers: List<FluidInventory>,
            player: InventoryPlayer?
        ): Boolean {
            for ((slot, fluidHandler) in fluidHandlers) {
                val drained = fluidHandler.drain(required.copy(), false) ?: continue
                if (drained.containsFluid(required)) {
                    fluidHandler.drain(required.copy(), true)
                    player?.setInventorySlotContents(slot, fluidHandler.container)
                    return true
                }
            }

            return false
        }

        fun searchAndRemoveContainFluid(
            inventory: MutableList<ItemStack>,
            fluidIngredients: MutableList<StructureIngredient.FluidIngredient>
        ) {
            val fluidHandlers = getFluidHandlerItems(inventory)
            val fluidIngredientIter: MutableIterator<StructureIngredient.FluidIngredient> = fluidIngredients.iterator()

            while (fluidIngredientIter.hasNext()) {
                val fluidIngredient = fluidIngredientIter.next()

                for (tuple in fluidIngredient.ingredientList()) {
                    val required = tuple.getFirst() as FluidStack
                    if (consumeInventoryFluid(required, fluidHandlers, null)) {
                        fluidIngredientIter.remove()
                        break
                    }
                }
            }
        }
    }

    var usingAE = false
    var ignoreFluids = false
    var missing = 0
    private var queue: Queue<BlockPos>? = null

    constructor() : super()

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
                    material.computeIfAbsent(info, l).add(Tuple(ingredient, state))
                }
            }
        }
    }

    private fun isFluid(state: IBlockState): Boolean {
        val block = state.getBlock()
        return block is BlockLiquid || block is BlockFluidBase
    }

    fun assemblyBlock(world: World, player: EntityPlayerMP): OperatingStatus {
        val pos = queue!!.poll() ?: return OperatingStatus.COMPLETE
        val info: BlockInformation = synchronized(pattern) {
            this.pattern.remove(pos) ?: return OperatingStatus.ALREADY_EXISTS
        }

        if (player.isCreative) {
            placeBlock(player, world, pos, info.sampleState)
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
                player.sendMessage(
                    TextComponentTranslation(
                        "message.assembly.tip.cannot_replace",
                        MiscUtils.posToString(pos)
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
            wobj?.actionableNode?.grid?.let {
                val grid = it.getCache<IStorageGrid>(IStorageGrid::class.java)
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
        }
        val itemInventory = player.inventory.mainInventory
        val fluidInventory = getFluidHandlerItems(itemInventory)
        for (ingredientAndIBlockState in list) {
            val ingredient = ingredientAndIBlockState.first
            if (ingredient.isItem) {
                if (ingredient.itemStack.isEmpty) continue
                if (MachineAssembly.consumeInventoryItem(
                        ingredient.itemStack,
                        itemInventory
                    )
                ) {
                    placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                    return OperatingStatus.SUCCESS
                }
            } else {
                if (consumeInventoryFluid(
                        ingredient.fluidStack,
                        fluidInventory,
                        player.inventory
                    )
                ) {
                    placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                    return OperatingStatus.SUCCESS
                }
            }
        }
        if (hasAE) {
            for (ingredientAndIBlockState in list) {
                val ingredient = ingredientAndIBlockState.first
                if (ingredient.isItem) {
                    if (ingredient.itemStack.isEmpty) continue
                    val item = items!!.extractItems(
                        ingredient.aEItemStack,
                        Actionable.MODULATE,
                        PlayerSource(player, wobj)
                    )
                    if (item == null || item.stackSize == 0L) continue
                    placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                    return OperatingStatus.SUCCESS
                } else {
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
        if (oldState.block == Blocks.AIR && matchesState(info, oldState)) {
            return OperatingStatus.ALREADY_EXISTS
        }
        if (missing > 0) {
            --missing
            return OperatingStatus.SUCCESS
        }
        player.sendMessage(
            TextComponentTranslation(
                "message.assembly.tip.missing",
                MiscUtils.posToString(pos)
            )
        )
        return OperatingStatus.FAILURE
    }

    private fun placeBlock(player: EntityPlayerMP, world: World, pos: BlockPos, state: IBlockState) {
        player.getServer()!!.addScheduledTask {
            if (!player.isCreative && ForgeEventFactory.onBlockPlace(
                    player,
                    BlockSnapshot(world, pos, state),
                    EnumFacing.UP
                ).isCanceled
            ) {
                player.sendMessage(
                    TextComponentTranslation(
                        "message.assembly.tip.missing",
                        MiscUtils.posToString(pos)
                    )
                )
                player.inventory.placeItemBackInInventory(
                    world,
                    StackUtils.getStackFromBlockState(state)
                )
            } else {
                val flags = 0b10011
                val chunk = world.getChunk(pos)
                var blockSnapshot: BlockSnapshot? = null
                if (world.captureBlockSnapshots)
                    blockSnapshot = BlockSnapshot.getBlockSnapshot(world, pos, flags)
                val iblockstate = chunk.setBlockState(pos, state)

                if (iblockstate != null) {
                    if (blockSnapshot == null) {
                        world.markAndNotifyBlock(pos, chunk, iblockstate, state, flags)
                    } else {
                        world.capturedBlockSnapshots.add(blockSnapshot)
                    }
                }
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