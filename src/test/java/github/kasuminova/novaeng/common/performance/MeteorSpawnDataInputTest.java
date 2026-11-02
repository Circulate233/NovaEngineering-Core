package github.kasuminova.novaeng.common.performance;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeteorSpawnDataInputTest {

    @TempDir
    Path directory;

    private static byte[] compressed(final NBTTagCompound compound) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(compound, bytes);
        return bytes.toByteArray();
    }

    @Test
    void actualCompressedNbtReadsRemainFreshAndCloseTheFile() throws Exception {
        final Path file = this.directory.resolve("meteor.dat");
        final Random random = new Random(0xAE2L);
        for (int i = 0; i < 20; i++) {
            final NBTTagCompound data = new NBTTagCompound();
            final byte[] values = new byte[i * 1000];
            random.nextBytes(values);
            data.setByteArray("payload", values);
            data.setInteger("revision", i);
            Files.write(file, compressed(data));
            final FileInputStream source = new FileInputStream(file.toFile());
            final NBTTagCompound actual = MeteorSpawnDataInput.read(source);
            assertEquals(data, actual);
            assertThrows(IOException.class, source::read);
            actual.setInteger("revision", -1);
            assertEquals(i, MeteorSpawnDataInput.read(new FileInputStream(file.toFile())).getInteger("revision"));
        }
    }

    @Test
    void malformedAndTruncatedFilesFailAndDoNotPoisonReusedBuffers() throws Exception {
        final NBTTagCompound expected = new NBTTagCompound();
        expected.setString("meteor", "fresh");
        final byte[] valid = compressed(expected);
        final Path file = this.directory.resolve("meteor.dat");
        for (final byte[] corrupt : new byte[][]{new byte[0], {1, 2, 3, 4}, Arrays.copyOf(valid, 11), Arrays.copyOf(valid, valid.length - 6)}) {
            Files.write(file, corrupt);
            final FileInputStream source = new FileInputStream(file.toFile());
            NBTTagCompound original = null;
            IOException originalFailure = null;
            try (final FileInputStream baseline = new FileInputStream(file.toFile())) {
                original = CompressedStreamTools.readCompressed(baseline);
            } catch (final IOException failure) {
                originalFailure = failure;
            }
            if (originalFailure != null) {
                assertThrows(IOException.class, () -> MeteorSpawnDataInput.read(source));
            } else {
                assertEquals(original, MeteorSpawnDataInput.read(source));
            }
            assertThrows(IOException.class, source::read);
            Files.write(file, valid);
            assertEquals(expected, MeteorSpawnDataInput.read(new FileInputStream(file.toFile())));
        }
    }

    @Test
    void customInputStreamsRetainTheOriginalReadPathAndCloseBehavior() throws Exception {
        final NBTTagCompound expected = new NBTTagCompound();
        expected.setInteger("value", 42);
        final ClosingInput stream = new ClosingInput(compressed(expected));
        assertEquals(expected, MeteorSpawnDataInput.read(stream));
        assertTrue(stream.closed);
    }

    @Test
    void fileBufferPreservesReadSkipAvailableBoundsAndBorrowOwnership() throws Exception {
        final byte[] bytes = new byte[30_000];
        new Random(42).nextBytes(bytes);
        final ClosingInput source = new ClosingInput(bytes);
        final MeteorSpawnDataInput.BufferedFile buffered = new MeteorSpawnDataInput.BufferedFile();
        buffered.attach(source);
        assertThrows(IllegalStateException.class, () -> buffered.attach(source));
        assertEquals(bytes[0] & 255, buffered.read());
        assertEquals(bytes.length - 1, buffered.available());
        assertEquals(100, buffered.skip(100));
        assertEquals(0, buffered.skip(-1));
        final byte[] remaining = buffered.readAllBytes();
        assertArrayEquals(Arrays.copyOfRange(bytes, 101, bytes.length), remaining);
        assertEquals(-1, buffered.read());
        assertEquals(0, buffered.read(new byte[0]));
        assertThrows(IndexOutOfBoundsException.class, () -> buffered.read(new byte[2], 1, 2));
        buffered.close();
        buffered.close();
        assertTrue(source.closed);
        assertThrows(IOException.class, buffered::read);
        buffered.attach(new ByteArrayInputStream(new byte[]{7}));
        assertEquals(7, buffered.read());
        assertEquals(-1, buffered.read());
        buffered.close();
    }

    @Test
    void gzipHeaderNoLongerIssuesOneUnderlyingReadPerByte() throws Exception {
        final byte[] bytes = new byte[200];
        final CountingInput source = new CountingInput(bytes);
        final MeteorSpawnDataInput.BufferedFile buffered = new MeteorSpawnDataInput.BufferedFile();
        buffered.attach(source);
        for (int i = 0; i < 10; i++) {
            assertEquals(0, buffered.read());
        }
        assertEquals(0, source.singleReads);
        assertEquals(1, source.bulkReads);
        buffered.close();
    }

    private static class ClosingInput extends ByteArrayInputStream {
        private boolean closed;

        private ClosingInput(final byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }
    }

    private static final class CountingInput extends ClosingInput {
        private int singleReads;
        private int bulkReads;

        private CountingInput(final byte[] bytes) {
            super(bytes);
        }

        @Override
        public int read() {
            this.singleReads++;
            return super.read();
        }

        @Override
        public int read(final byte[] bytes, final int offset, final int length) {
            this.bulkReads++;
            return super.read(bytes, offset, length);
        }
    }
}
