package github.kasuminova.novaeng.common.container;

import github.kasuminova.novaeng.common.tile.ecotech.ecalculator.ECalculatorController;
import hellfirepvp.modularmachinery.common.container.ContainerBase;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.EntityPlayer;

public class ContainerECalculatorController extends ContainerBase<ECalculatorController> {

    @Setter
    @Getter
    protected int tickExisted = 0;

    public ContainerECalculatorController(final ECalculatorController owner, final EntityPlayer opening) {
        super(owner, opening);
    }

    @Override
    protected void addPlayerSlots(final EntityPlayer opening) {
        // No player slots
    }

}
