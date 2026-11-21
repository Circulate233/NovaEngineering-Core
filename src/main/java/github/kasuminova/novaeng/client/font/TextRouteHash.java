package github.kasuminova.novaeng.client.font;

/**
 * Same nine-element Arrays.hashCode result without a varargs array or boxing.
 */
public final class TextRouteHash {
    private TextRouteHash() {
    }

    public static int hash(final String source, final int argb, final boolean shadow, final int fontSizeBits,
                           final long revision, final long structuredRevision, final int rasterScaleBits,
                           final int engineBits, final long backendState) {
        int hash = 31 + (source == null ? 0 : source.hashCode());
        hash = 31 * hash + argb;
        hash = 31 * hash + Boolean.hashCode(shadow);
        hash = 31 * hash + fontSizeBits;
        hash = 31 * hash + Long.hashCode(revision);
        hash = 31 * hash + Long.hashCode(structuredRevision);
        hash = 31 * hash + rasterScaleBits;
        hash = 31 * hash + engineBits;
        return 31 * hash + Long.hashCode(backendState);
    }
}
