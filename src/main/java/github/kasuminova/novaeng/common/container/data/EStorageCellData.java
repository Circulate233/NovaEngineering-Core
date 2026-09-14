package github.kasuminova.novaeng.common.container.data;

import ae2.api.storage.StorageCells;
import ae2.api.storage.cells.StorageCell;
import ae2.api.storage.cells.StorageCellStatistics;
import ae2.util.inv.AppEngCellInventory;
import github.kasuminova.novaeng.common.block.ecotech.estorage.prop.DriveStorageLevel;
import github.kasuminova.novaeng.common.block.ecotech.estorage.prop.DriveStorageType;
import github.kasuminova.novaeng.common.item.estorage.EStorageCell;
import github.kasuminova.novaeng.common.tile.ecotech.estorage.EStorageCellDrive;
import net.minecraft.item.ItemStack;

public record EStorageCellData(DriveStorageType type, DriveStorageLevel level, int usedTypes, long usedBytes) {

    public static EStorageCellData from(final EStorageCellDrive drive) {
        AppEngCellInventory driveInv = drive.getDriveInv();
        ItemStack stack = driveInv.getStackInSlot(0);
        if (stack.isEmpty()) {
            return null;
        }
        StorageCell cellInventory = StorageCells.getCellInventory(stack, null);
        if (!(cellInventory instanceof StorageCellStatistics statistics)) {
            return null;
        }
        EStorageCell cell = (EStorageCell) stack.getItem();
        DriveStorageType type = EStorageCellDrive.getCellType(cell);
        if (type == null) {
            return null;
        }
        DriveStorageLevel level = cell.getLevel();
        return new EStorageCellData(type, level, (int) statistics.getStoredTypes(), statistics.getUsedBytes());
    }

}
