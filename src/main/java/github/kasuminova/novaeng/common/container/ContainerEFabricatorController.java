package github.kasuminova.novaeng.common.container;

import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorController;
import hellfirepvp.modularmachinery.common.container.ContainerBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;

public class ContainerEFabricatorController extends ContainerBase<EFabricatorController> {
    
    protected int tickExisted = 0;

    public ContainerEFabricatorController(final EFabricatorController owner, final EntityPlayer opening) {
        super(owner, opening);
    }

    public int getTickExisted() {
        return tickExisted;
    }

    public void setTickExisted(final int tickExisted) {
        this.tickExisted = tickExisted;
    }

    @Override
    protected void addPlayerSlots(EntityPlayer opening) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(opening.inventory, j + i * 9 + 9, 18 + j * 18, 119 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(opening.inventory, i, 18 + i * 18, 177));
        }
    }

}
