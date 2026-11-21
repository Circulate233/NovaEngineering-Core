package github.kasuminova.novaeng.common.performance.string;

/**
 * Independent lazy budgets keep high-cardinality text out of the namespace/token indexes.
 */
public final class StringCanon {
    private StringCanon() {
    }

    public static WeakStringTable namespaces() {
        return Namespaces.TABLE;
    }

    public static WeakStringTable locations() {
        return Locations.TABLE;
    }

    public static WeakStringTable tokens() {
        return Tokens.TABLE;
    }

    public static String value(final String value) {
        return Values.TABLE.canonicalize(value);
    }

    private static final class Namespaces {
        static final WeakStringTable TABLE = new WeakStringTable(2048, 256);
    }

    private static final class Locations {
        static final WeakStringTable TABLE = new WeakStringTable(131072, 2048);
    }

    private static final class Tokens {
        static final WeakStringTable TABLE = new WeakStringTable(16384, 512);
    }

    private static final class Values {
        static final WeakStringTable TABLE = new WeakStringTable(65536, 2048);
    }
}
