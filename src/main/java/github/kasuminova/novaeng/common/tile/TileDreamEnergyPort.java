package github.kasuminova.novaeng.common.tile;

import github.kasuminova.novaeng.common.block.BlockDreamEnergyPort;
import github.kasuminova.novaeng.common.handler.DreamEnergyPortHandler;
import github.kasuminova.novaeng.common.machine.DreamEnergyCore;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import sonar.fluxnetworks.FluxConfig;
import sonar.fluxnetworks.api.network.ITransferHandler;
import sonar.fluxnetworks.common.tileentity.TileFluxStorage;

public class TileDreamEnergyPort extends TileFluxStorage {

    private final DreamEnergyPortHandler handler;
    private final ItemStack stack;
    private BlockPos ctrlPos;

    public BlockPos getCtrlPos() {
        return this.ctrlPos;
    }

    public TileDreamEnergyPort() {
        this.customName = "Dream Energy Port";
        this.limit = (long) FluxConfig.gargantuanTransfer;
        this.handler = new DreamEnergyPortHandler(this);
        this.stack = new ItemStack(BlockDreamEnergyPort.INSTANCE);
    }

    @Override
    public long getMaxTransferLimit() {
        if (ctrlPos == null || !getCtrlStructureFormed()) {
            return 0;
        }
        return Long.MAX_VALUE;
    }

    @Override
    public ItemStack getDisplayStack() {
        return this.writeStorageToDisplayStack(stack);
    }

    @NotNull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (ctrlPos != null){
            compound.setLong("ctrlPos",this.ctrlPos.toLong());
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("ctrlPos")){
            this.ctrlPos = BlockPos.fromLong(compound.getLong("ctrlPos"));
        }
    }

    @Override
    public ITransferHandler getTransferHandler() {
        return this.handler;
    }

    public boolean getCtrlStructureFormed(){
        if (this.world.getTileEntity(ctrlPos) instanceof TileMultiblockMachineController ctrl) {
            if (ctrl.getFoundMachine() != null && ctrl.getFoundMachine().getRegistryName().equals(DreamEnergyCore.REGISTRY_NAME)){
                return ctrl.isStructureFormed();
            }
        }
        return false;
    }

    public void setCtrlPos(BlockPos pos){
        this.ctrlPos = pos;
        this.handler.setCtrlPos(pos);
        this.handler.setWorld(this.world);
    }
}
