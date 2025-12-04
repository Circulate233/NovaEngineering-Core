package github.kasuminova.novaeng.common.tile.ecotech.efabricator

import net.minecraft.nbt.NBTTagCompound

class EFabricatorTail : EFabricatorPart() {

    private var formed: Boolean = false

    fun isFormed(): Boolean {
        return formed
    }

    override fun onAssembled() {
        if (!formed) {
            formed = true
            markForUpdateSync()
        }
        super.onAssembled()
    }

    override fun onDisassembled() {
        if (formed) {
            formed = false
            markForUpdateSync()
        }
        super.onDisassembled()
    }

    override fun readCustomNBT(compound: NBTTagCompound) {
        formed = compound.getBoolean("formed")
        super.readCustomNBT(compound)
    }

    override fun writeCustomNBT(compound: NBTTagCompound) {
        compound.setBoolean("formed", formed)
        super.writeCustomNBT(compound)
    }
}
