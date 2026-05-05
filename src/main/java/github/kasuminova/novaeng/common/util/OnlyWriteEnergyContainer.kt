package github.kasuminova.novaeng.common.util

import net.minecraftforge.energy.IEnergyStorage

class OnlyWriteEnergyContainer : IEnergyStorage {

    private var storage : IEnergyStorage

    constructor(storage: IEnergyStorage) {
        this.storage = storage
    }

    fun setStorage(storage: IEnergyStorage) : OnlyWriteEnergyContainer {
        this.storage = storage
        return this
    }

    override fun receiveEnergy(i: Int, bl: Boolean): Int {
        return storage.receiveEnergy(i, bl)
    }

    override fun extractEnergy(i: Int, bl: Boolean): Int {
        return 0
    }

    override fun getEnergyStored(): Int {
        return storage.energyStored
    }

    override fun getMaxEnergyStored(): Int {
        return storage.maxEnergyStored
    }

    override fun canExtract(): Boolean {
        return false
    }

    override fun canReceive(): Boolean {
        return storage.canReceive()
    }
}