package github.kasuminova.novaeng.client.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** A cached metadata probe can close the selected resource without opening its data stream. */
final class DeferredResourceInputStream extends InputStream {
    private ModelJsonMetadataIndex.StreamOpener opener;
    private InputStream delegate;

    DeferredResourceInputStream(final ModelJsonMetadataIndex.StreamOpener opener) {
        this.opener = opener;
    }

    private InputStream stream() throws IOException {
        if (delegate == null) {
            if (opener == null) {
                throw new IOException("Resource stream closed");
            }
            delegate = opener.open();
            opener = null;
        }
        return delegate;
    }

    @Override public int read() throws IOException { return stream().read(); }
    @Override public int read(final byte[] b, final int off, final int len) throws IOException {
        return stream().read(b, off, len);
    }
    @Override public long skip(final long count) throws IOException { return stream().skip(count); }
    @Override public int available() throws IOException { return stream().available(); }
    @Override public void reset() throws IOException { stream().reset(); }

    @Override
    public boolean markSupported() {
        try {
            return stream().markSupported();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void mark(final int limit) {
        try {
            stream().mark(limit);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        opener = null;
        if (delegate != null) {
            final InputStream opened = delegate;
            delegate = null;
            opened.close();
        }
    }
}
