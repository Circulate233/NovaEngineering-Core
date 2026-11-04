package github.kasuminova.novaeng.common.item.estorage;

import ae2.api.stacks.AEKeyType;
import github.kasuminova.novaeng.novaeng_core.Tags;
import me.ramidzkh.mekae2.ae2.AEGasKeyType;
import github.kasuminova.novaeng.common.block.ecotech.estorage.prop.DriveStorageLevel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class EStorageCellGas extends EStorageCell {

    public static final EStorageCellGas LEVEL_A = new EStorageCellGas(DriveStorageLevel.A, 16, 4);
    public static final EStorageCellGas LEVEL_B = new EStorageCellGas(DriveStorageLevel.B, 64, 16);
    public static final EStorageCellGas LEVEL_C = new EStorageCellGas(DriveStorageLevel.C, 256, 64);

    public EStorageCellGas(final DriveStorageLevel level, final int millionBytes, final int byteMultiplier) {
        super(level, millionBytes, byteMultiplier);
        setRegistryName(new ResourceLocation(Tags.MOD_ID, "estorage_cell_gas_" + millionBytes + "m"));
        setTranslationKey(Tags.MOD_ID + '.' + "estorage_cell_gas_" + millionBytes + "m");
    }

    @Override
    public int getTotalTypes(@NotNull ItemStack itemStack) {
        return 25;
    }

    @Override
    public int getBytesPerType(@NotNull ItemStack itemStack) {
        return byteMultiplier * 1024;
    }


    @NotNull
    @Override
    public AEKeyType getKeyType() {
        return AEGasKeyType.TYPE;
    }
}
