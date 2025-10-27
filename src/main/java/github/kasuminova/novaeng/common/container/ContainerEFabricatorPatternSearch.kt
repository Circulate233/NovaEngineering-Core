package github.kasuminova.novaeng.common.container

import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorController
import hellfirepvp.modularmachinery.common.container.ContainerBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Slot

open class ContainerEFabricatorPatternSearch(owner: EFabricatorController?, opening: EntityPlayer?) :
    ContainerBase<EFabricatorController?>(owner, opening) {
    override fun addPlayerSlots(opening: EntityPlayer) {
        for (i in 0..2) {
            for (j in 0..8) {
                addSlotToContainer(Slot(opening.inventory, j + i * 9 + 9, 18 + j * 18, (119 + 11) + i * 18))
            }
        }
        for (i in 0..8) {
            addSlotToContainer(Slot(opening.inventory, i, 18 + i * 18, 177 + 11))
        }
    }
}
