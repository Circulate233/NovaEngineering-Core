package github.kasuminova.novaeng.common.block.ecotech.estorage.prop;

import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum DriveStorageLevel implements IStringSerializable {
    EMPTY("empty"),
    A("a"),
    B("b"),
    C("c");

    public static final PropertyEnum<DriveStorageLevel> STORAGE_LEVEL = PropertyEnum.create("storage_level", DriveStorageLevel.class);

    /**
     * Shared lookup table: {@code values()} clones its backing array on every call.
     */
    private static final DriveStorageLevel[] BY_ORDINAL = values();
    private final String name;

    DriveStorageLevel(String name) {
        this.name = name;
    }

    /**
     * Resolves a network-supplied ordinal without the per-call clone of {@code values()}.
     */
    public static DriveStorageLevel byOrdinal(final int ordinal) {
        final DriveStorageLevel[] all = BY_ORDINAL;
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }
}
