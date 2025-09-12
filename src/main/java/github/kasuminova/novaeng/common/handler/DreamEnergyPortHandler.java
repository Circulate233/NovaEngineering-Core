package github.kasuminova.novaeng.common.handler;

import github.kasuminova.novaeng.common.machine.DreamEnergyCore;
import github.kasuminova.novaeng.common.tile.TileDreamEnergyPort;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import lombok.Setter;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import sonar.fluxnetworks.common.connection.transfer.BasicTransferHandler;

public class DreamEnergyPortHandler extends BasicTransferHandler<TileDreamEnergyPort> {
    private long removed;
    @Setter
    private BlockPos ctrlPos;
    @Setter
    private World world;

    public DreamEnergyPortHandler(TileDreamEnergyPort device) {
        super(device);
        this.ctrlPos = device.getCtrlPos();
        this.world = device.getWorld();
    }

    @Override
    public void onCycleStart() {
    }

    @Override
    public void onCycleEnd() {
        this.removed = 0L;
    }

    @Override
    public void updateTransfers(EnumFacing... enumFacings) {

    }

    public void addToBuffer(long energy) {
        if (energy > 0L) {
            DreamEnergyCore.receiveEnergy(getCtrl(),1,energy);
            ((TileDreamEnergyPort)this.device).markServerEnergyChanged();
        }
    }

    public long removeFromBuffer(long energy) {
        long a = Math.min(Math.min(energy, this.getBuffer()), Math.max(Long.MAX_VALUE - this.removed,0));
        if (a <= 0L) {
            return 0L;
        } else {
            DreamEnergyCore.extractEnergy(getCtrl(),1,a);
            this.removed += a;
            ((TileDreamEnergyPort)this.device).markServerEnergyChanged();
            return a;
        }
    }

    @Override
    public long getRequest() {
        if (!getCtrlStructureFormed()){
            return 0;
        }
        return Long.MAX_VALUE;
    }

    public TileMultiblockMachineController getCtrl(){
        if (ctrlPos == null || this.world == null){
            return null;
        }
        if (this.world.getTileEntity(ctrlPos) instanceof TileMultiblockMachineController ctrl) {
            if (ctrl.getFoundMachine() != null && ctrl.getFoundMachine().getRegistryName().equals(DreamEnergyCore.REGISTRY_NAME)){
                return ctrl;
            }
        }
        return null;
    }

    public boolean getCtrlStructureFormed(){
        var ctrl = getCtrl();
        if (ctrl != null) {
            return ctrl.isStructureFormed();
        }
        return false;
    }
}
