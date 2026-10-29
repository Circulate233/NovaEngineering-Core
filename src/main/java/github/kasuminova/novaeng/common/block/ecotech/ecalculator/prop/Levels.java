package github.kasuminova.novaeng.common.block.ecotech.ecalculator.prop;

public enum Levels {

    L4,
    L6,
    L9,
    L11;

    /**
     * Shared lookup table: {@code values()} clones its backing array on every call.
     */
    public static final Levels[] BY_ORDINAL = values();

    Levels() {
    }

    /**
     * Resolves a stored ordinal without the per-call clone of {@code values()}.
     */
    public static Levels byOrdinal(final int ordinal) {
        final Levels[] all = BY_ORDINAL;
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }

}
