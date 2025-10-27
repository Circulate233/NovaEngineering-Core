package github.kasuminova.novaeng.common.container

import github.kasuminova.novaeng.common.tile.machine.SingularityCore
import hellfirepvp.modularmachinery.common.container.ContainerBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Slot

open class ContainerSingularityCore(owner: SingularityCore?, opening: EntityPlayer?) :
    ContainerBase<SingularityCore?>(owner, opening) {

    protected var tickExisted: Int = 0

    override fun addPlayerSlots(opening: EntityPlayer) {
        for (i in 0..2) {
            for (j in 0..8) {
                addSlotToContainer(Slot(opening.inventory, j + i * 9 + 9, 119 + j * 18, 184 + i * 18))
            }
        }
        for (i in 0..8) {
            addSlotToContainer(Slot(opening.inventory, i, 119 + i * 18, 242))
        }
    }
}
