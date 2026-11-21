package github.kasuminova.novaeng.common.performance.string;

public final class ResourceStringCodec {
    private ResourceStringCodec() {
    }

    public static String[] split(final String value) {
        return split(value, value.length(), null, false);
    }

    public static String[] model(final String value) {
        final int hash = value.indexOf('#');
        final String variant = hash >= 0 ? StringCanon.locations().slice(value, hash + 1, value.length()) : null;
        // Preserve 1.12's handling of '#' at offsets zero and one.
        return split(value, hash > 1 ? hash : value.length(), variant, true);
    }

    private static String[] split(final String value, final int end, final String variant, final boolean model) {
        final int colon = value.indexOf(':');
        final boolean found = colon >= 0 && colon < end;
        final String namespace = found && colon > 1 ? StringCanon.namespaces().slice(value, 0, colon) : "minecraft";
        final String path = StringCanon.locations().slice(value, found ? colon + 1 : 0, end);
        return model ? new String[]{namespace, path, variant} : new String[]{namespace, path};
    }
}
