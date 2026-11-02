package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class MeteorSpawnDataInput {

    private static final ThreadLocal<ObjectArrayList<BufferedFile>> IDLE = ThreadLocal.withInitial(() -> new ObjectArrayList<>(2));

    private MeteorSpawnDataInput() {
    }

    public static NBTTagCompound read(final InputStream source) throws IOException {
        if (source.getClass() != FileInputStream.class) {
            return CompressedStreamTools.readCompressed(source);
        }
        final ObjectArrayList<BufferedFile> idle = IDLE.get();
        final BufferedFile input = idle.isEmpty() ? new BufferedFile() : idle.removeLast();
        input.attach(source);
        try {
            return CompressedStreamTools.readCompressed(input);
        } finally {
            try {
                input.close();
            } finally {
                if (idle.size() < 2) {
                    idle.add(input);
                }
            }
        }
    }

    /**
     * Exclusively borrowed until the complete NBT read returns, even if GZIP closes it earlier.
     */
    static final class BufferedFile extends InputStream {
        private final byte[] buffer = new byte[8192];
        private InputStream source;
        private int index;
        private int limit;

        void attach(final InputStream source) {
            if (this.source != null) {
                throw new IllegalStateException("File buffer is already borrowed");
            }
            this.source = Objects.requireNonNull(source);
            this.index = 0;
            this.limit = 0;
        }

        @Override
        public int read() throws IOException {
            this.ensureOpen();
            if (this.index == this.limit && !this.fill()) {
                return -1;
            }
            return this.buffer[this.index++] & 255;
        }

        @Override
        public int read(final byte[] target, final int offset, final int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, target.length);
            this.ensureOpen();
            if (length == 0) {
                return 0;
            }
            if (this.index == this.limit) {
                if (length >= this.buffer.length) {
                    return this.source.read(target, offset, length);
                }
                if (!this.fill()) {
                    return -1;
                }
            }
            final int count = Math.min(length, this.limit - this.index);
            System.arraycopy(this.buffer, this.index, target, offset, count);
            this.index += count;
            return count;
        }

        @Override
        public long skip(final long count) throws IOException {
            this.ensureOpen();
            if (count <= 0) {
                return 0;
            }
            final int buffered = (int) Math.min(count, this.limit - this.index);
            this.index += buffered;
            return buffered == count ? buffered : buffered + this.source.skip(count - buffered);
        }

        @Override
        public int available() throws IOException {
            this.ensureOpen();
            return (int) Math.min(Integer.MAX_VALUE, (long) this.limit - this.index + this.source.available());
        }

        @Override
        public void close() throws IOException {
            final InputStream closing = this.source;
            this.source = null;
            this.index = 0;
            this.limit = 0;
            if (closing != null) {
                closing.close();
            }
        }

        private boolean fill() throws IOException {
            this.limit = this.source.read(this.buffer);
            this.index = 0;
            if (this.limit < 0) {
                this.limit = 0;
                return false;
            }
            return true;
        }

        private void ensureOpen() throws IOException {
            if (this.source == null) {
                throw new IOException("Stream closed");
            }
        }
    }
}
