package github.kasuminova.novaeng.common.container.slot

import github.kasuminova.novaeng.common.container.ContainerModularServerAssembler
import github.kasuminova.novaeng.common.hypernet.computer.ModularServer
import github.kasuminova.novaeng.common.hypernet.computer.assembly.AssemblyInvCPUConst
import github.kasuminova.novaeng.common.hypernet.computer.assembly.AssemblyInvCalculateCardConst
import github.kasuminova.novaeng.common.hypernet.computer.assembly.AssemblyInvExtensionConst
import github.kasuminova.novaeng.common.hypernet.computer.assembly.AssemblyInvPowerConst
import github.kasuminova.novaeng.common.util.TileItemHandler
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import javax.annotation.Nonnull

open class AssemblySlotManager(private val modularServer: ModularServer) {
    private val inventorySlots: MutableMap<String?, Int2ObjectMap<SlotConditionItemHandler>> =
        HashMap()

    fun initSlots() {
        addCPUSlots()
        addCalculateCardSlots()
        addExtensionCardSlots()
        addPowerSlots()
    }

    protected fun addCPUSlots() {
        val invCPU = modularServer.getInvByName("cpu")

        val ext2 =
            addSlot<SlotCPUExtItemHandler?>(SlotCPUExtItemHandler(AssemblyInvCPUConst.CPU_EXTENSION_SLOT_ID, invCPU))

        // Default CPU Slots
        for (i in 0..1) {
            addSlot<SlotCPUItemHandler?>(SlotCPUItemHandler(i, i, invCPU))!!
                .softDependsOn(
                    addSlot<SlotCPUHeatRadiatorItemHandler?>(
                        SlotCPUHeatRadiatorItemHandler(
                            i,
                            AssemblyInvCPUConst.CPU_HEAT_RADIATOR_SLOT_ID_START + i,
                            invCPU
                        )
                    )
                )
        }
        // Extension CPU Slots
        for (i in 0..1) {
            addSlot<SlotCPUItemHandler?>(SlotCPUItemHandler(2 + i, 2 + i, invCPU))!!
                .softDependsOn(
                    addSlot<SlotCPUHeatRadiatorItemHandler?>(
                        SlotCPUHeatRadiatorItemHandler(
                            2 + i,
                            AssemblyInvCPUConst.CPU_HEAT_RADIATOR_SLOT_ID_START + 2 + i,
                            invCPU
                        )
                    )!!
                        .dependsOn(ext2)
                )
                .dependsOn(ext2)
        }

        // Default RAM Slots
        for (i in 0..7) {
            addSlot<SlotRAMItemHandler?>(SlotRAMItemHandler(i, AssemblyInvCPUConst.RAM_SLOT_ID_START + i, invCPU))!!
                .softDependsOn(
                    addSlot<SlotRAMHeatRadiatorItemHandler?>(
                        SlotRAMHeatRadiatorItemHandler(
                            i,
                            AssemblyInvCPUConst.RAM_HEAT_RADIATOR_SLOT_ID_START + i,
                            invCPU
                        )
                    )
                )
        }
        // Extension RAM Slots
        for (i in 0..7) {
            addSlot<SlotRAMItemHandler?>(
                SlotRAMItemHandler(
                    8 + i,
                    AssemblyInvCPUConst.RAM_SLOT_ID_START + 8 + i,
                    invCPU
                )
            )!!
                .softDependsOn(
                    addSlot<SlotRAMHeatRadiatorItemHandler?>(
                        SlotRAMHeatRadiatorItemHandler(
                            8 + i,
                            AssemblyInvCPUConst.RAM_HEAT_RADIATOR_SLOT_ID_START + 8 + i,
                            invCPU
                        )
                    )!!
                        .dependsOn(ext2)
                )
                .dependsOn(ext2)
        }
    }

    protected fun addCalculateCardSlots() {
        val invCalculateCard = modularServer.getInvByName("calculate_card")

        for (extSlotID in 0..<AssemblyInvCalculateCardConst.LINES) {
            val calculateCardExt = addSlot<SlotCalculateCardExtItemHandler?>(
                SlotCalculateCardExtItemHandler(
                    extSlotID, AssemblyInvCalculateCardConst.EXT_SLOT_ID_START + extSlotID, invCalculateCard
                )
            )

            for (slotID in 0..<AssemblyInvCalculateCardConst.LINE_SLOTS) {
                val id = (extSlotID * AssemblyInvCalculateCardConst.LINE_SLOTS) + slotID
                addSlot<SlotConditionItemHandler?>(
                    SlotCalculateCardItemHandler(id, id, invCalculateCard)
                        .softDependsOn(
                            addSlot<SlotCalculateCardHeatRadiatorItemHandler?>(
                                SlotCalculateCardHeatRadiatorItemHandler(
                                    id,
                                    AssemblyInvCalculateCardConst.HEAT_RADIATOR_SLOT_ID_START + id,
                                    invCalculateCard
                                )
                            )!!
                                .dependsOn(calculateCardExt)
                        )
                )!!
                    .dependsOn(calculateCardExt)
            }
        }
    }

    protected fun addExtensionCardSlots() {
        val invExtensionCard = modularServer.getInvByName("extension")

        for (extSlotID in 0..<AssemblyInvExtensionConst.LINES) {
            val calculateCardExt = addSlot<SlotExtensionCardExtItemHandler?>(
                SlotExtensionCardExtItemHandler(
                    extSlotID, AssemblyInvExtensionConst.EXT_SLOT_ID_START + extSlotID, invExtensionCard
                )
            )

            for (slotID in 0..<AssemblyInvExtensionConst.LINE_SLOTS) {
                val id = (extSlotID * AssemblyInvExtensionConst.LINE_SLOTS) + slotID
                addSlot<SlotConditionItemHandler?>(
                    SlotExtensionCardItemHandler(id, id, invExtensionCard)
                        .softDependsOn(
                            addSlot<SlotExtensionCardHeatRadiatorItemHandler?>(
                                SlotExtensionCardHeatRadiatorItemHandler(
                                    id,
                                    AssemblyInvExtensionConst.HEAT_RADIATOR_SLOT_ID_START + id,
                                    invExtensionCard
                                )
                            )!!
                                .dependsOn(calculateCardExt)
                        )
                )!!
                    .dependsOn(calculateCardExt)
            }
        }
    }

    protected fun addPowerSlots() {
        val invPower = modularServer.getInvByName("power")

        for (slotID in 0..3) {
            addSlot<SlotPSUItemHandler?>(SlotPSUItemHandler(slotID, slotID, invPower))
        }
        for (slotID in 0..3) {
            addSlot<SlotCapacitorItemHandler?>(
                SlotCapacitorItemHandler(
                    slotID,
                    AssemblyInvPowerConst.CAPACITOR_SLOT_ID_START + slotID,
                    invPower
                )
            )
        }
    }

    fun <SLOT : SlotConditionItemHandler?> addSlot(@Nonnull slot: SLOT?): SLOT {
        val inv = slot!!.itemHandler
        val slotID = slot.slotIndex
        inventorySlots.computeIfAbsent(inv.invName) { v: String? -> Int2ObjectOpenHashMap() }
            .put(slotID, slot)
        return slot
    }

    fun addSlot(
        @Nonnull slot: SlotConditionItemHandler,
        @Nonnull tileItemHandler: TileItemHandler,
        @Nonnull invName: String,
        slotId: Int
    ) {
        if (tileItemHandler.isSlotAvailable(slotId)) {
            inventorySlots.computeIfAbsent(invName) { v: String? -> Int2ObjectOpenHashMap() }
                .put(slotId, slot)
        }
    }

    fun addAllSlotToContainer(container: ContainerModularServerAssembler) {
        for (invSlots in inventorySlots.values) {
            for (slot in invSlots.values) {
                container.addSlotToContainer(slot)
            }
        }
    }

    fun getSlot(@Nonnull invName: String, slotId: Int): SlotConditionItemHandler? {
        val invSlots = inventorySlots[invName]
        if (invSlots != null) {
            val slot = invSlots.get(slotId)
            if (slot.itemHandler.isSlotAvailable(slotId)) {
                return slot
            }
        }
        return null
    }
}
