package github.kasuminova.novaeng.common.util;

import net.minecraftforge.energy.IEnergyStorage;

public final class OnlyWriteEnergyContainer implements IEnergyStorage {

    private IEnergyStorage storage;

    public OnlyWriteEnergyContainer(final IEnergyStorage storage) {
        this.storage = storage;
    }

    public OnlyWriteEnergyContainer setStorage(final IEnergyStorage storage) {
        this.storage = storage;
        return this;
    }

    @Override
    public int receiveEnergy(final int amount, final boolean simulate) {
        return this.storage.receiveEnergy(amount, simulate);
    }

    @Override
    public int extractEnergy(final int amount, final boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return this.storage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return this.storage.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return this.storage.canReceive();
    }
}
