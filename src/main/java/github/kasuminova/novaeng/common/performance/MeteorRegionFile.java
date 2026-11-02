package github.kasuminova.novaeng.common.performance;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Publishes a complete compressed region without truncating its previous contents.
 */
public final class MeteorRegionFile {
    private MeteorRegionFile() {
    }

    public static BasicFileAttributes write(final File file, final NBTTagCompound data) throws IOException {
        Path target = file.toPath().toAbsolutePath();
        if (Files.isSymbolicLink(target)) target = target.toRealPath();
        final Path temporary = Files.createTempFile(target.getParent(), ".nova-meteor-", ".tmp");
        try {
            try (final OutputStream output = Files.newOutputStream(temporary)) {
                CompressedStreamTools.writeCompressed(data, output);
            }
            final BasicFileAttributes version = Files.readAttributes(temporary, BasicFileAttributes.class);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (final AtomicMoveNotSupportedException unavailable) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return version;
        } catch (final IOException | RuntimeException | Error failure) {
            try {
                Files.deleteIfExists(temporary);
            } catch (final IOException | RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }
}
