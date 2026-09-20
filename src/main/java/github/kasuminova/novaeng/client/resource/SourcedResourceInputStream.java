package github.kasuminova.novaeng.client.resource;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * Transparent resource stream wrapper carrying the winning pack's identity and mutability.
 */
public final class SourcedResourceInputStream extends FilterInputStream implements SourcedResourceStream {

    private final Object sourceIdentity;
    private final boolean cacheableSource;

    /**
     * Wraps a resource stream without altering reads, marks, skips, or close behavior.
     *
     * @param delegate original resource stream
     * @param sourceIdentity concrete winning resource pack
     * @param cacheableSource whether that pack is static for the current reload
     */
    public SourcedResourceInputStream(final InputStream delegate,
                                      final Object sourceIdentity,
                                      final boolean cacheableSource) {
        super(delegate);
        this.sourceIdentity = sourceIdentity;
        this.cacheableSource = cacheableSource;
    }

    @Override
    public Object sourceIdentity() {
        return this.sourceIdentity;
    }

    @Override
    public boolean isCacheableSource() {
        return this.cacheableSource;
    }
}
