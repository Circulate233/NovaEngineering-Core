package github.kasuminova.novaeng.common.item.estorage;

import ae2.api.stacks.AEKeyType;
import github.kasuminova.novaeng.common.block.ecotech.estorage.prop.DriveStorageLevel;
import github.kasuminova.novaeng.novaeng_core.Tags;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class EStorageCellFluid extends EStorageCell {

    public static final EStorageCellFluid LEVEL_A = new EStorageCellFluid(DriveStorageLevel.A, 16, 4);
    public static final EStorageCellFluid LEVEL_B = new EStorageCellFluid(DriveStorageLevel.B, 64, 16);
    public static final EStorageCellFluid LEVEL_C = new EStorageCellFluid(DriveStorageLevel.C, 256, 64);

    public EStorageCellFluid(final DriveStorageLevel level, final int millionBytes, final int byteMultiplier) {
        super(level, millionBytes, byteMultiplier);
        setRegistryName(new ResourceLocation(Tags.MOD_ID, "estorage_cell_fluid_" + millionBytes + "m"));
        setTranslationKey(Tags.MOD_ID + '.' + "estorage_cell_fluid_" + millionBytes + "m");
    }

    @Override
    public int getTotalTypes(@Nonnull final ItemStack cellItem) {
        return 25;
    }

    @Override
    public int getBytesPerType(@Nonnull final ItemStack cellItem) {
        return byteMultiplier * 1024;
    }

    @Nonnull
    @Override
    public AEKeyType getKeyType() {
        return AEKeyType.fluids();
    }
}
