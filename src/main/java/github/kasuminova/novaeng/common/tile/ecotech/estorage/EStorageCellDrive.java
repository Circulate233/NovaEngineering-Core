package github.kasuminova.novaeng.common.tile.ecotech.estorage;

import ae2.api.inventories.InternalInventory;
import ae2.api.stacks.AEKeyType;
import ae2.api.storage.StorageCells;
import ae2.api.storage.cells.ISaveProvider;
import ae2.api.storage.cells.StorageCell;
import ae2.api.storage.cells.StorageCellStatistics;
import ae2.util.inv.AppEngCellInventory;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.InternalInventoryHost;
import ae2.util.inv.filter.IAEItemFilter;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.block.ecotech.estorage.BlockEStorageController;
import github.kasuminova.novaeng.common.block.ecotech.estorage.prop.DriveStorageCapacity;
import github.kasuminova.novaeng.common.block.ecotech.estorage.prop.DriveStorageLevel;
import github.kasuminova.novaeng.common.block.ecotech.estorage.prop.DriveStorageType;
import github.kasuminova.novaeng.common.container.data.EStorageCellData;
import github.kasuminova.novaeng.common.estorage.ECellDriveWatcher;
import github.kasuminova.novaeng.common.item.estorage.EStorageCell;
import github.kasuminova.novaeng.common.item.estorage.EStorageCellFluid;
import github.kasuminova.novaeng.common.item.estorage.EStorageCellGas;
import github.kasuminova.novaeng.common.item.estorage.EStorageCellItem;
import github.kasuminova.novaeng.common.network.PktCellDriveStatusUpdate;
import hellfirepvp.modularmachinery.common.base.Mods;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class EStorageCellDrive extends EStoragePart implements ISaveProvider, InternalInventoryHost {

    @Getter
    protected final AppEngCellInventory driveInv = new AppEngCellInventory(this, 1);
    protected final Map<AEKeyType, ECellDriveWatcher> inventoryHandlers = new Reference2ObjectOpenHashMap<>();

    @Getter
    protected StorageCell cellHandler = null;
    @Getter
    protected ECellDriveWatcher watcher = null;

    protected boolean isCached = false;

    protected long lastWriteTick = 0;
    @Setter
    @Getter
    protected boolean writing = false;

    public EStorageCellDrive() {
        this.driveInv.setFilter(CellInvFilter.INSTANCE);
    }

    public static int getMaxTypes(final EStorageCellData data) {
        return switch (data.type()) {
            case EMPTY -> 0;
            case ITEM -> 315;
            case FLUID -> 25;
            case GAS -> Mods.MEKENG.isPresent() ? 25 : 0;
        };
    }

    public static long getMaxBytes(final EStorageCellData data) {
        DriveStorageType type = data.type();
        DriveStorageLevel level = data.level();
        return switch (type) {
            case EMPTY -> 0;
            case ITEM -> switch (level) {
                case EMPTY -> 0;
                case A -> EStorageCellItem.LEVEL_A.getBytes(ItemStack.EMPTY);
                case B -> EStorageCellItem.LEVEL_B.getBytes(ItemStack.EMPTY);
                case C -> EStorageCellItem.LEVEL_C.getBytes(ItemStack.EMPTY);
            };
            case FLUID -> switch (level) {
                case EMPTY -> 0;
                case A -> EStorageCellFluid.LEVEL_A.getBytes(ItemStack.EMPTY);
                case B -> EStorageCellFluid.LEVEL_B.getBytes(ItemStack.EMPTY);
                case C -> EStorageCellFluid.LEVEL_C.getBytes(ItemStack.EMPTY);
            };
            case GAS -> Mods.MEKENG.isPresent() ? switch (level) {
                case EMPTY -> 0;
                case A -> EStorageCellGas.LEVEL_A.getBytes(ItemStack.EMPTY);
                case B -> EStorageCellGas.LEVEL_B.getBytes(ItemStack.EMPTY);
                case C -> EStorageCellGas.LEVEL_C.getBytes(ItemStack.EMPTY);
            } : 0;
        };
    }

    public static DriveStorageType getCellType(final EStorageCell cell) {
        DriveStorageType type;
        switch (cell) {
            case EStorageCellItem _ -> type = DriveStorageType.ITEM;
            case EStorageCellFluid _ -> type = DriveStorageType.FLUID;
            case EStorageCellGas _ when Mods.MEKENG.isPresent() -> type = DriveStorageType.GAS;
            case null, default -> {
                return null;
            }
        }
        return type;
    }

    public static DriveStorageCapacity getCapacity(final StorageCell cellInventory) {
        if (!(cellInventory instanceof StorageCellStatistics statistics)) {
            return DriveStorageCapacity.EMPTY;
        }
        long totalTypes = statistics.getTotalTypes();
        long storedTypes = statistics.getStoredTypes();
        if (storedTypes == 0) {
            return DriveStorageCapacity.EMPTY;
        }
        if (statistics.getUsedBytes() >= statistics.getTotalBytes()) {
            return DriveStorageCapacity.FULL;
        }
        if (storedTypes >= totalTypes) {
            return DriveStorageCapacity.TYPE_MAX;
        }
        return DriveStorageCapacity.EMPTY;
    }

    public void updateWriteState() {
        long totalWorldTime = world.getTotalWorldTime();
        boolean changed = false;
        if (totalWorldTime - lastWriteTick >= 40) {
            if (writing) {
                writing = false;
                changed = true;
            }
        } else if (!writing) {
            writing = true;
            changed = true;
        }
        if (cellHandler == null) {
            return;
        }
        // Static update or changed update.
        if (world.getTotalWorldTime() % 200 == 0) {
            BlockPos pos = getPos();
            NovaEngineeringCore.NET_CHANNEL.sendToAllTracking(
                new PktCellDriveStatusUpdate(getPos(), writing),
                new NetworkRegistry.TargetPoint(
                    world.provider.getDimension(),
                    pos.getX(), pos.getY(), pos.getZ(),
                    -1)
            );
        } else if (changed) {
            BlockPos pos = getPos();
            NovaEngineeringCore.NET_CHANNEL.sendToAllAround(
                new PktCellDriveStatusUpdate(getPos(), writing),
                new NetworkRegistry.TargetPoint(
                    world.provider.getDimension(),
                    pos.getX(), pos.getY(), pos.getZ(),
                    16)
            );
        }
    }

    protected void updateHandler(final boolean refreshState) {
        if (isCached) {
            return;
        }
        watcher = null;
        cellHandler = null;
        inventoryHandlers.clear();
        isCached = true;
        driveInv.setHandler(0, null);
        ItemStack stack = driveInv.getStackInSlot(0);
        if (stack.isEmpty()) {
            updateDriveBlockState();
            return;
        }
        StorageCell cellInventory = StorageCells.getCellInventory(stack, this);
        if (cellInventory != null && stack.getItem() instanceof EStorageCell cell) {
            cellHandler = cellInventory;
            driveInv.setHandler(0, cellInventory);
            watcher = new ECellDriveWatcher(cellInventory, this);
            inventoryHandlers.put(cell.getKeyType(), watcher);
        }
        if (partController != null) {
            partController.recalculateEnergyUsage();
        }

        if (cellInventory == null || !refreshState) {
            return;
        }
        updateDriveBlockState();
    }

    public boolean isCellSupported(final DriveStorageLevel level) {
        if (partController == null) {
            return false;
        }
        if (level == DriveStorageLevel.A) {
            BlockEStorageController parent = partController.getParentController();
            return parent == BlockEStorageController.L4 || parent == BlockEStorageController.L6 || parent == BlockEStorageController.L9;
        }
        if (level == DriveStorageLevel.B) {
            BlockEStorageController parent = partController.getParentController();
            return parent == BlockEStorageController.L6 || parent == BlockEStorageController.L9;
        }
        if (level == DriveStorageLevel.C) {
            BlockEStorageController parent = partController.getParentController();
            return parent == BlockEStorageController.L9;
        }
        return false;
    }

    public void updateDriveBlockState() {
        if (world == null) {
            return;
        }
        markForUpdate();
    }

    public ECellDriveWatcher getHandler(final AEKeyType channel) {
        updateHandler(false);
        if (driveInv.getStackInSlot(0).getItem() instanceof EStorageCell cell && isCellSupported(cell.getLevel())) {
            return inventoryHandlers.get(channel);
        }
        return null;
    }

    @Override
    public void onChangeInventory(final AppEngInternalInventory inv, final int slot) {
        this.isCached = false; // recalculate the storage cell.
        this.updateHandler(true);
        this.markForUpdateSync();
    }

    @Override
    public void saveChangedInventory(final AppEngInternalInventory inv) {
        saveChanges();
    }

    @Override
    public boolean isClientSide() {
        return world.isRemote;
    }

    @Override
    public void onDisassembled() {
        super.onDisassembled();
        driveInv.persist();
    }

    public void onWriting() {
        this.lastWriteTick = world.getTotalWorldTime();
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
    }

    @Override
    public void saveChanges() {
        markDirty();
    }

    @Override
    public void markDirty() {
        markChunkDirty();
    }

    private static class CellInvFilter implements IAEItemFilter {

        private static final CellInvFilter INSTANCE = new CellInvFilter();

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return !stack.isEmpty()
                && stack.getItem() instanceof EStorageCell
                && StorageCells.isCellHandled(stack);
        }

    }
}
