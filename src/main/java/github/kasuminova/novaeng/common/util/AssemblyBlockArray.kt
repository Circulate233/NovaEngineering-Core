package github.kasuminova.novaeng.common.util

import ae2.api.config.Actionable
import ae2.api.networking.security.IActionHost
import ae2.container.AEBaseContainer
import ae2.api.storage.MEStorage
import ae2.me.helpers.PlayerSource
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

class AssemblyBlockArray : BlockArray {

    enum class PlacementStage {
        SOLID,
        FLUID
    }

    data class QueuedPlacement(
        val pos: BlockPos,
        val info: BlockInformation,
        val stage: PlacementStage
    )

    private data class FluidInventory(val slot: Int, val fluid: IFluidHandlerItem)

    companion object {
        private val material =
            Object2ReferenceOpenHashMap<BlockInformation, ObjectArrayList<Tuple<Ingredient, IBlockState>>>()
        private val placementStages =
            Object2ReferenceOpenHashMap<BlockInformation, PlacementStage>()

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

        private fun buildMaterialList(info: BlockInformation): ObjectArrayList<Tuple<Ingredient, IBlockState>> {
            val resolved = ObjectArrayList<Tuple<Ingredient, IBlockState>>()
            var fluidOnly = true

            for (stateDescriptor in info.matchingStates) {
                for (state in stateDescriptor.applicable) {
                    val block = state.block
                    val ingredient = if (block is BlockFluidBase) {
                        Ingredient(FluidStack(block.fluid, 1000))
                    } else if (block is BlockLiquid) {
                        val material = state.material
                        if (material === Material.LAVA) {
                            Ingredient(FluidStack(FluidRegistry.LAVA, 1000))
                        } else {
                            Ingredient(FluidStack(FluidRegistry.WATER, 1000))
                        }
                    } else {
                        fluidOnly = false
                        Ingredient(StackUtils.getStackFromBlockState(state))
                    }
                    if (ingredient.isItem) {
                        fluidOnly = false
                    }
                    resolved.add(Tuple(ingredient, state))
                }
            }

            material[info] = resolved
            placementStages[info] = if (resolved.isNotEmpty() && fluidOnly) {
                PlacementStage.FLUID
            } else {
                PlacementStage.SOLID
            }
            return resolved
        }

        fun getMaterialList(info: BlockInformation): ObjectArrayList<Tuple<Ingredient, IBlockState>> {
            return material[info] ?: buildMaterialList(info)
        }

        fun getPlacementStage(info: BlockInformation): PlacementStage {
            return placementStages[info] ?: run {
                buildMaterialList(info)
                placementStages[info] ?: PlacementStage.SOLID
            }
        }
    }

    var usingAE = false
    var ignoreFluids = false
    var missing = 0
    private var queue: Queue<QueuedPlacement>? = null

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
        this.usingAE = usingAE
        this.ignoreFluids = ignoreFluids

        val solidQueue = ArrayDeque<QueuedPlacement>()
        val fluidQueue = ArrayDeque<QueuedPlacement>()
        for ((pos, info) in this.pattern.entries) {
            getMaterialList(info)
            val stage = getPlacementStage(info)
            if (ignoreFluids && stage == PlacementStage.FLUID) {
                continue
            }
            val placement = QueuedPlacement(pos, info, stage)
            if (stage == PlacementStage.FLUID) {
                fluidQueue.add(placement)
            } else {
                solidQueue.add(placement)
            }
        }

        queue = ArrayDeque<QueuedPlacement>(solidQueue.size + fluidQueue.size).also {
            it.addAll(solidQueue)
            it.addAll(fluidQueue)
        }
    }

    private fun pollPlacement(): QueuedPlacement? {
        while (true) {
            val next = queue?.poll() ?: return null
            if (synchronized(pattern) { this.pattern.remove(next.pos) } != null) {
                return next
            }
        }
    }

    private fun isFluid(state: IBlockState): Boolean {
        val block = state.getBlock()
        return block is BlockLiquid || block is BlockFluidBase
    }

    fun assemblyBlock(world: World, player: EntityPlayerMP): OperatingStatus {
        val placement = pollPlacement() ?: return OperatingStatus.COMPLETE
        val pos = placement.pos
        val info = placement.info

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

        val list = getMaterialList(info)

        var hasAE = false
        var terminal: IActionHost? = null
        var storage: MEStorage? = null

        if (usingAE) {
            terminal = (player.openContainer as? AEBaseContainer)?.target as? IActionHost
            terminal?.actionableNode?.grid()?.let { grid ->
                storage = grid.storageService.inventory
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
                    val key = ingredient.aeItemKey ?: continue
                    val item = storage!!.extract(key, ingredient.itemStack.count.toLong(), Actionable.MODULATE,
                        PlayerSource(player, terminal))
                    if (item == 0L) continue
                    placeBlock(player, world, pos, ingredientAndIBlockState.getSecond())
                    return OperatingStatus.SUCCESS
                } else {
                    val key = ingredient.aeFluidKey ?: continue
                    val fluid = storage!!.extract(key, 1000, Actionable.SIMULATE, PlayerSource(player, terminal))
                    if (fluid < 1000) continue
                    storage!!.extract(key, 1000, Actionable.MODULATE, PlayerSource(player, terminal))
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
