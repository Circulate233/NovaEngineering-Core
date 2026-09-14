package github.kasuminova.novaeng.common.tile.ecotech.ecalculator;

import ae2.api.inventories.InternalInventory;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.InternalInventoryHost;
import ae2.util.inv.filter.IAEItemFilter;
import github.kasuminova.novaeng.common.block.ecotech.ecalculator.prop.DriveStorageLevel;
import github.kasuminova.novaeng.common.block.ecotech.ecalculator.prop.Levels;
import github.kasuminova.novaeng.common.item.ecalculator.ECalculatorCell;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ECalculatorCellDrive extends ECalculatorPart implements InternalInventoryHost {

    protected final AppEngInternalInventory driveInv = new AppEngInternalInventory(this, 1);
    protected EnumFacing connectedSide = null;

    public ECalculatorCellDrive() {
        this.driveInv.setFilter(CellInvFilter.INSTANCE);
    }

    @Override
    public void onChangeInventory(final AppEngInternalInventory inv, final int slot) {
        disconnectTransmitter();
        final ECalculatorController controller = getController();
        if (controller != null) {
            controller.recalculateTotalBytes();
            controller.createVirtualCPU();
        }
        this.markForUpdateSync();
    }

    @Override
    public void saveChangedInventory(final AppEngInternalInventory inv) {
        markDirty();
    }

    @Override
    public boolean isClientSide() {
        return world != null && world.isRemote;
    }

    public long getSuppliedBytes() {
        final ItemStack stackInSlot = driveInv.getStackInSlot(0);
        if (stackInSlot.isEmpty()) {
            return 0;
        }
        if (!(stackInSlot.getItem() instanceof ECalculatorCell cell)) {
            return 0;
        }

        Levels level = getControllerLevel();
        DriveStorageLevel cellLevel = cell.getLevel();
        switch (cellLevel) {
            case B -> {
                if (level == Levels.L4) {
                    return 0;
                }
            }
            case C -> {
                if (level == Levels.L4 || level == Levels.L6) {
                    return 0;
                }
            }
        }

        return cell.getTotalBytes();
    }

    public boolean connectTransmitter(final EnumFacing side, final Levels level) {
        ItemStack stackInSlot = driveInv.getStackInSlot(0);
        if (stackInSlot.isEmpty() || !(stackInSlot.getItem() instanceof ECalculatorCell cell)) {
            return false;
        }

        DriveStorageLevel cellLevel = cell.getLevel();
        switch (cellLevel) {
            case B -> {
                if (level == Levels.L4) {
                    return false;
                }
            }
            case C -> {
                if (level == Levels.L4 || level == Levels.L6) {
                    return false;
                }
            }
        }

        if (this.connectedSide != side) {
            this.connectedSide = side;
            markForUpdateSync();
        }
        return true;
    }

    public void disconnectTransmitter() {
        if (this.connectedSide != null) {
            this.connectedSide = null;
            markForUpdateSync();
        }
    }

    @Override
    public void onDisassembled() {
        super.onDisassembled();
        disconnectTransmitter();
    }

    @Override
    public void onAssembled() {
        super.onAssembled();
    }

    public AppEngInternalInventory getDriveInv() {
        return driveInv;
    }

    public EnumFacing getConnectedSide() {
        return connectedSide;
    }

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(driveInv.toItemHandler());
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readCustomNBT(final NBTTagCompound tag) {
        super.readCustomNBT(tag);

        final NBTTagCompound opt = tag.getCompoundTag("driveInv");
        for (int x = 0; x < driveInv.size(); x++) {
            final NBTTagCompound item = opt.getCompoundTag("item" + x);
            driveInv.setItemDirect(x, item.hasKey("id", 8) ? new ItemStack(item) : ItemStack.EMPTY);
        }
        if (tag.hasKey("connectedSide")) {
            this.connectedSide = EnumFacing.values()[tag.getByte("connectedSide")];
        } else {
            this.connectedSide = null;
        }

        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            notifyUpdate();
        }
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound tag) {
        super.writeCustomNBT(tag);

        final NBTTagCompound opt = new NBTTagCompound();
        for (int x = 0; x < driveInv.size(); x++) {
            final NBTTagCompound itemNBT = new NBTTagCompound();
            final ItemStack is = driveInv.getStackInSlot(x);
            if (!is.isEmpty()) {
                is.writeToNBT(itemNBT);
            }
            opt.setTag("item" + x, itemNBT);
        }
        tag.setTag("driveInv", opt);

        if (this.connectedSide != null) {
            tag.setByte("connectedSide", (byte) this.connectedSide.ordinal());
        }
    }

    @Override
    public void notifyUpdate() {
        super.notifyUpdate();
        if (world == null) {
            return;
        }
        world.notifyNeighborsOfStateChange(getPos(), world.getBlockState(getPos()).getBlock(), false);
    }

    @Override
    public void markDirty() {
        markChunkDirty();
    }

    private static class CellInvFilter implements IAEItemFilter {

        private static final CellInvFilter INSTANCE = new CellInvFilter();

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof ECalculatorCell;
        }

    }

}
