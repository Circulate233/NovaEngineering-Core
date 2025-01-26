package github.kasuminova.novaeng.common.machine.Drills;

import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import github.kasuminova.novaeng.common.machine.MachineSpecial;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;

public abstract class Drill implements MachineSpecial {

    @Override
    public void init(final DynamicMachine machine) {
        machine.addMachineEventHandler(MachineStructureUpdateEvent.class, event -> {
            TileMultiblockMachineController controller = event.getController();
            controller.setWorkMode(TileMultiblockMachineController.WorkMode.SEMI_SYNC);
        });
    }
}
