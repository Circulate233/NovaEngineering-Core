package github.kasuminova.novaeng.common.container.slot;

import github.kasuminova.novaeng.common.container.ContainerModularServerAssembler;
import github.kasuminova.novaeng.common.hypernet.computer.ModularServer;
import github.kasuminova.novaeng.common.hypernet.computer.assembly.AssemblyInvCPUConst;
import github.kasuminova.novaeng.common.hypernet.computer.assembly.AssemblyInvCalculateCardConst;
import github.kasuminova.novaeng.common.hypernet.computer.assembly.AssemblyInvExtensionConst;
import github.kasuminova.novaeng.common.hypernet.computer.assembly.AssemblyInvPowerConst;
import github.kasuminova.novaeng.common.util.TileItemHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class AssemblySlotManager {

    private final ModularServer modularServer;
    private final Map<String, Int2ObjectMap<SlotConditionItemHandler>> inventorySlots = new HashMap<>();

    public AssemblySlotManager(final ModularServer modularServer) {
        this.modularServer = modularServer;
    }

    public void initSlots() {
        this.addCPUSlots();
        this.addCalculateCardSlots();
        this.addExtensionCardSlots();
        this.addPowerSlots();
    }

    protected void addCPUSlots() {
        final TileItemHandler inventory = this.modularServer.getInvByName("cpu");
        final var extension = this.addSlot(new SlotCPUExtItemHandler(AssemblyInvCPUConst.CPU_EXTENSION_SLOT_ID, inventory));
        for (int i = 0; i < 2; i++) {
            this.addSlot(new SlotCPUItemHandler(i, i, inventory)).softDependsOn(this.addSlot(
                new SlotCPUHeatRadiatorItemHandler(i, AssemblyInvCPUConst.CPU_HEAT_RADIATOR_SLOT_ID_START + i, inventory)));
        }
        for (int i = 0; i < 2; i++) {
            this.addSlot(new SlotCPUItemHandler(2 + i, 2 + i, inventory)).softDependsOn(this.addSlot(
                                                                                                new SlotCPUHeatRadiatorItemHandler(2 + i, AssemblyInvCPUConst.CPU_HEAT_RADIATOR_SLOT_ID_START + 2 + i, inventory))
                                                                                            .dependsOn(extension)).dependsOn(extension);
        }
        for (int i = 0; i < 8; i++) {
            this.addSlot(new SlotRAMItemHandler(i, AssemblyInvCPUConst.RAM_SLOT_ID_START + i, inventory)).softDependsOn(this.addSlot(
                new SlotRAMHeatRadiatorItemHandler(i, AssemblyInvCPUConst.RAM_HEAT_RADIATOR_SLOT_ID_START + i, inventory)));
        }
        for (int i = 0; i < 8; i++) {
            this.addSlot(new SlotRAMItemHandler(8 + i, AssemblyInvCPUConst.RAM_SLOT_ID_START + 8 + i, inventory)).softDependsOn(this.addSlot(
                                                                                                                                        new SlotRAMHeatRadiatorItemHandler(8 + i, AssemblyInvCPUConst.RAM_HEAT_RADIATOR_SLOT_ID_START + 8 + i, inventory))
                                                                                                                                    .dependsOn(extension)).dependsOn(extension);
        }
    }

    protected void addCalculateCardSlots() {
        final TileItemHandler inventory = this.modularServer.getInvByName("calculate_card");
        for (int line = 0; line < AssemblyInvCalculateCardConst.LINES; line++) {
            final var extension = this.addSlot(new SlotCalculateCardExtItemHandler(line,
                AssemblyInvCalculateCardConst.EXT_SLOT_ID_START + line, inventory));
            for (int slot = 0; slot < AssemblyInvCalculateCardConst.LINE_SLOTS; slot++) {
                final int id = line * AssemblyInvCalculateCardConst.LINE_SLOTS + slot;
                this.addSlot(new SlotCalculateCardItemHandler(id, id, inventory).softDependsOn(this.addSlot(
                                                                                                       new SlotCalculateCardHeatRadiatorItemHandler(id, AssemblyInvCalculateCardConst.HEAT_RADIATOR_SLOT_ID_START + id, inventory))
                                                                                                   .dependsOn(extension))).dependsOn(extension);
            }
        }
    }

    protected void addExtensionCardSlots() {
        final TileItemHandler inventory = this.modularServer.getInvByName("extension");
        for (int line = 0; line < AssemblyInvExtensionConst.LINES; line++) {
            final var extension = this.addSlot(new SlotExtensionCardExtItemHandler(line,
                AssemblyInvExtensionConst.EXT_SLOT_ID_START + line, inventory));
            for (int slot = 0; slot < AssemblyInvExtensionConst.LINE_SLOTS; slot++) {
                final int id = line * AssemblyInvExtensionConst.LINE_SLOTS + slot;
                this.addSlot(new SlotExtensionCardItemHandler(id, id, inventory).softDependsOn(this.addSlot(
                                                                                                       new SlotExtensionCardHeatRadiatorItemHandler(id, AssemblyInvExtensionConst.HEAT_RADIATOR_SLOT_ID_START + id, inventory))
                                                                                                   .dependsOn(extension))).dependsOn(extension);
            }
        }
    }

    protected void addPowerSlots() {
        final TileItemHandler inventory = this.modularServer.getInvByName("power");
        for (int slot = 0; slot < 4; slot++) {
            this.addSlot(new SlotPSUItemHandler(slot, slot, inventory));
        }
        for (int slot = 0; slot < 4; slot++) {
            this.addSlot(new SlotCapacitorItemHandler(slot, AssemblyInvPowerConst.CAPACITOR_SLOT_ID_START + slot, inventory));
        }
    }

    public <S extends SlotConditionItemHandler> S addSlot(final S slot) {
        final TileItemHandler inventory = slot.getItemHandler();
        this.inventorySlots.computeIfAbsent(inventory.getInvName(), ignored -> new Int2ObjectOpenHashMap<>())
                           .put(slot.getSlotIndex(), slot);
        return slot;
    }

    public void addSlot(final SlotConditionItemHandler slot, final TileItemHandler inventory, final String name, final int slotId) {
        if (inventory.isSlotAvailable(slotId)) {
            this.inventorySlots.computeIfAbsent(name, ignored -> new Int2ObjectOpenHashMap<>()).put(slotId, slot);
        }
    }

    public void addAllSlotToContainer(final ContainerModularServerAssembler container) {
        for (final var slots : this.inventorySlots.values()) {
            for (final var slot : slots.values()) {
                container.addSlotToContainer(slot);
            }
        }
    }

    @Nullable
    public SlotConditionItemHandler getSlot(final String name, final int slotId) {
        final var slots = this.inventorySlots.get(name);
        if (slots != null) {
            final var slot = slots.get(slotId);
            if (slot.getItemHandler().isSlotAvailable(slotId)) {
                return slot;
            }
        }
        return null;
    }
}
