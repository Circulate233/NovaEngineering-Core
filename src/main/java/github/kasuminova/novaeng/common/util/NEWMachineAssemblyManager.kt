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
import appeng.me.helpers.PlayerSource
import appeng.util.item.AEItemStack
import com.circulation.random_complement.common.util.MEHandler
import com.glodblock.github.common.item.fake.FakeFluids
import hellfirepvp.modularmachinery.ModularMachinery
import hellfirepvp.modularmachinery.common.integration.ModIntegrationJEI
import hellfirepvp.modularmachinery.common.integration.preview.StructurePreviewWrapper
import hellfirepvp.modularmachinery.common.machine.DynamicMachine
import hellfirepvp.modularmachinery.common.network.PktAssemblyReport
import hellfirepvp.modularmachinery.common.util.BlockArray
import hellfirepvp.modularmachinery.common.util.ItemUtils
import ink.ikx.mmce.common.assembly.MachineAssembly
import ink.ikx.mmce.common.utils.StructureIngredient
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.util.Tuple
import net.minecraftforge.fluids.FluidStack
import java.util.stream.Collectors

class NEWMachineAssemblyManager {

    data class BlockPair(val block: Block, val meta: Int) {
        val blockState: IBlockState
            get() : IBlockState {
                @Suppress("DEPRECATION")
                return block.getStateFromMeta(meta)
            }
    }

    companion object {
        private val ADDITIONAL_CONSTRUCTORS by lazy {
            Reference2ObjectOpenHashMap<Block, Int2ObjectOpenHashMap<DynamicMachine>>()
        }
        private val MACHINE_ASSEMBLY_CACHE by lazy {
            Object2ObjectOpenHashMap<EntityPlayer, AssemblyBlockArray>()
        }
        private val CheckAllItemComplete by lazy {
            ObjectLists.singleton(
                ObjectLists.singleton(ItemStack.EMPTY)
            )
        }
        private val emptyMiss2ListPair by lazy {
            Miss2ListPair(0, ObjectLists.emptyList())
        }

        fun getAllDynamicMachines(): Collection<DynamicMachine> {
            val i = ObjectArrayList<DynamicMachine>()
            for (map in ADDITIONAL_CONSTRUCTORS.values) {
                i.addAll(map.values)
            }
            return i
        }

        fun getDynamicMachine(state: IBlockState): DynamicMachine? {
            return getDynamicMachine(state.block, state.block.getMetaFromState(state))
        }

        fun getDynamicMachine(block: Block, meta: Int): DynamicMachine? {
            return ADDITIONAL_CONSTRUCTORS[block]?.let { it[meta] }
        }

        fun getConstructorsIterator(): ObjectIterator<Map.Entry<Block, Int2ObjectOpenHashMap<DynamicMachine>>> {
            return ADDITIONAL_CONSTRUCTORS.entries.iterator()
        }

        fun setConstructors(block: BlockPair, machine: NEWDynamicMachine) {
            ADDITIONAL_CONSTRUCTORS.computeIfAbsent(block.block) { Int2ObjectOpenHashMap() }[block.meta] = machine
            ModIntegrationJEI.PREVIEW_WRAPPERS.add(StructurePreviewWrapper(machine))
        }

        fun addAssemblyMachine(player: EntityPlayer, array: BlockArray): AssemblyBlockArray {
            val newArray = AssemblyBlockArray(array)
            MACHINE_ASSEMBLY_CACHE[player] = newArray
            return newArray
        }

        fun getMachineAssembly(player: EntityPlayer): AssemblyBlockArray? {
            return MACHINE_ASSEMBLY_CACHE[player]
        }

        fun checkMachineAssembly(player: EntityPlayer): Boolean {
            return MACHINE_ASSEMBLY_CACHE.containsKey(player)
        }

        fun removeMachineAssembly(player: EntityPlayer) {
            MACHINE_ASSEMBLY_CACHE.remove(player)?.end()
        }

        fun checkAllItems(
            player: EntityPlayer,
            ingredient: StructureIngredient,
            usingAE: Boolean,
            autoAECrafting: Boolean
        ): Miss2ListPair {
            if (player.isCreative) return emptyMiss2ListPair
            val inventory = player.inventory.mainInventory.stream().map { obj: ItemStack -> obj.copy() }
                .collect(Collectors.toCollection { ObjectArrayList() })
            val itemIngredientList = ingredient.itemIngredient()
            val fluidIngredientList = ingredient.fluidIngredient()
            MachineAssembly.searchAndRemoveContainItem(inventory, itemIngredientList)
            MachineAssembly.searchAndRemoveContainFluid(inventory, fluidIngredientList)
            if (itemIngredientList.isEmpty() && fluidIngredientList.isEmpty()) {
                return emptyMiss2ListPair
            } else {
                val itemStackIngList = getItemStackIngList(itemIngredientList)
                val fluidStackIngList = getFluidStackIngList(fluidIngredientList)

                if (usingAE) {
                    val obj = MEHandler.getTerminalGuiObject(player)
                    obj?.actionableNode?.grid?.let {
                        val storage = it.getCache<IStorageGrid>(IStorageGrid::class.java)
                        val items: IMEMonitor<IAEItemStack?> = storage.getInventory(
                            AEApi.instance().storage().getStorageChannel(IItemStorageChannel::class.java)
                        )
                        val fluids = storage.getInventory(
                            AEApi.instance().storage().getStorageChannel(IFluidStorageChannel::class.java)
                        )
                        val playerSource = PlayerSource(player, obj)
                        for (stacks in ReferenceArrayList(itemStackIngList)) {
                            for (item in ReferenceArrayList(stacks)) {
                                val aeItem = items.extractItems(
                                    AEItemStack.fromItemStack(item),
                                    Actionable.SIMULATE,
                                    playerSource
                                )
                                if (aeItem == null) continue
                                val aeItemSize = aeItem.stackSize.toInt()
                                if (item.count <= aeItemSize) {
                                    itemStackIngList.remove(stacks)
                                    break
                                } else {
                                    for (stack in stacks) {
                                        stack.shrink(aeItemSize)
                                    }
                                }
                            }
                        }
                        for (stacks in ReferenceArrayList(fluidStackIngList)) {
                            for (fluid in ReferenceArrayList(stacks)) {
                                val aeFluid = fluids.extractItems(
                                    AEFluidStack.fromFluidStack(fluid),
                                    Actionable.SIMULATE,
                                    playerSource
                                )
                                if (aeFluid == null) continue
                                val aeFluidSize = aeFluid.stackSize.toInt()
                                if (fluid.amount <= aeFluidSize) {
                                    fluidStackIngList.remove(stacks)
                                    break
                                } else {
                                    for (stack in stacks) {
                                        stack.amount -= aeFluidSize
                                    }
                                }
                            }
                        }
                    }
                    if (itemStackIngList.isEmpty() && fluidStackIngList.isEmpty()) {
                        return emptyMiss2ListPair
                    }
                }

                var miss = 0
                for (stacks in itemStackIngList) {
                    for (stack in stacks) {
                        miss += stack.count
                        break
                    }
                }
                for (stacks in fluidStackIngList) {
                    for (stack in stacks) {
                        miss += (stack.amount + 999) / 1000
                        break
                    }
                }

                val pkt = PktAssemblyReport(itemStackIngList, fluidStackIngList)
                if (player is EntityPlayerMP) {
                    ModularMachinery.NET_CHANNEL.sendTo(pkt, player)
                }

                return if (autoAECrafting) {
                    val list = ObjectArrayList(itemStackIngList)
                    for (stacks in fluidStackIngList) {
                        val fs = ObjectArrayList<ItemStack>()
                        for (stack in stacks) {
                            val i = FakeFluids.packFluid2Drops(stack)
                            i.count = stack.amount
                            fs.add(i)
                        }
                        list.add(fs)
                    }

                    Miss2ListPair(miss, list)
                } else Miss2ListPair(miss, CheckAllItemComplete)
            }
        }

        class Miss2ListPair(val miss: Int, val list: List<List<ItemStack>>)

        private fun getFluidStackIngList(fluidIngredientList: List<StructureIngredient.FluidIngredient>): MutableList<MutableList<FluidStack>> {
            val fluidStackIngList: MutableList<MutableList<FluidStack>> = ObjectArrayList()

            label36@ for (ingredient in fluidIngredientList) {
                if (!ingredient.ingredientList().isEmpty()) {
                    val stackIngList = ingredient.ingredientList().stream()
                        .map { obj: Tuple<FluidStack, IBlockState> -> obj.getFirst() }
                        .collect(Collectors.toCollection { ObjectArrayList() })
                    if (stackIngList.size == 1) {
                        val ing = stackIngList[0]

                        for (fluidStackList in fluidStackIngList) {
                            if (fluidStackList.size == 1) {
                                val another = fluidStackList[0]
                                if (ing.isFluidEqual(another)) {
                                    another.amount += 1000
                                    continue@label36
                                }
                            }
                        }
                    }

                    fluidStackIngList.add(stackIngList)
                }
            }

            return fluidStackIngList
        }

        private fun getItemStackIngList(itemIngredientList: List<StructureIngredient.ItemIngredient>): MutableList<MutableList<ItemStack>> {
            val stackList: MutableList<MutableList<ItemStack>> = ObjectArrayList()

            label56@ for (itemIng in itemIngredientList) {
                if (!itemIng.ingredientList().isEmpty()) {
                    val stackIngList: MutableList<ItemStack> = itemIng.ingredientList().stream()
                        .map { obj: Tuple<ItemStack, IBlockState> -> obj.getFirst() }
                        .collect(Collectors.toCollection { ObjectArrayList() })
                    if (stackIngList.size == 1) {
                        val ing = stackIngList[0]

                        for (itemStackList in stackList) {
                            if (itemStackList.size == 1) {
                                val anotherInput = itemStackList[0]
                                if (ItemUtils.matchStacks(ing, anotherInput)) {
                                    anotherInput.grow(1)
                                    continue@label56
                                }
                            }
                        }
                    }

                    val filteredStackIngList: MutableList<ItemStack> = ObjectArrayList()

                    label44@ for (stack in stackIngList) {
                        for (filtered in filteredStackIngList) {
                            if (ItemUtils.matchStacks(stack, filtered)) {
                                continue@label44
                            }
                        }

                        filteredStackIngList.add(stack)
                    }

                    stackList.add(filteredStackIngList)
                }
            }

            return stackList
        }
    }

    enum class OperatingStatus {
        SUCCESS,
        ALREADY_EXISTS,
        FAILURE,
        COMPLETE
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

        val itemStack: ItemStack
            get() = ingredient as ItemStack

        val aEItemStack: IAEItemStack?
            get() = aeStack as? IAEItemStack

        val fluidStack: FluidStack
            get() = ingredient as FluidStack

        val aEFluidStack: IAEFluidStack?
            get() = aeStack as? IAEFluidStack
    }
}