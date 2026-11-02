package github.kasuminova.novaeng.common.tile.ecotech.efabricator;

import net.minecraft.nbt.NBTTagCompound;

public final class EFabricatorTail extends EFabricatorPart {

    private boolean formed;

    public boolean isFormed() {
        return this.formed;
    }

    @Override
    public void onAssembled() {
        if (!this.formed) {
            this.formed = true;
            this.markForUpdateSync();
        }
        super.onAssembled();
    }

    @Override
    public void onDisassembled() {
        if (this.formed) {
            this.formed = false;
            this.markForUpdateSync();
        }
        super.onDisassembled();
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        this.formed = compound.getBoolean("formed");
        super.readCustomNBT(compound);
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        compound.setBoolean("formed", this.formed);
        super.writeCustomNBT(compound);
    }
}
