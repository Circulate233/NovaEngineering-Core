package github.kasuminova.novaeng.common.block.ecotech.estorage.prop;

import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum DriveStorageType implements IStringSerializable {
    EMPTY("empty"),
    ITEM("item"),
    FLUID("fluid"),
    GAS("gas");

    public static final PropertyEnum<DriveStorageType> STORAGE_TYPE = PropertyEnum.create("storage_type", DriveStorageType.class);

    /**
     * Shared lookup table: {@code values()} clones its backing array on every call.
     */
    private static final DriveStorageType[] BY_ORDINAL = values();
    private final String name;

    DriveStorageType(String name) {
        this.name = name;
    }

    /**
     * Resolves a network-supplied ordinal without the per-call clone of {@code values()}.
     */
    public static DriveStorageType byOrdinal(final int ordinal) {
        final DriveStorageType[] all = BY_ORDINAL;
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }
}
