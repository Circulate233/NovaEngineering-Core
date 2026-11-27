package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;

/**
 * Read-only archive handles shared by the ZenUtils bytecode providers while each provider owns a lease.
 */
public final class ScriptClasspathArchives {
    private static final Map<ArchiveKey, Archive> ARCHIVES = new Object2ObjectOpenHashMap<>();
    private static final Map<FileSystem, Archive> LEASES = new Reference2ObjectOpenHashMap<>();

    private ScriptClasspathArchives() {
    }

    public static FileSystem open(final Path source, final ClassLoader loader) throws IOException {
        final ArchiveKey key = loader == null ? archiveKey(source) : null;
        if (key == null) return FileSystems.newFileSystem(source, loader);

        synchronized (ARCHIVES) {
            final Archive existing = ARCHIVES.get(key);
            if (existing != null && existing.fileSystem.isOpen()) {
                existing.references++;
                return existing.fileSystem;
            }
            final FileSystem fileSystem = FileSystems.newFileSystem(source, loader);
            final Archive archive = new Archive(key, fileSystem);
            ARCHIVES.put(key, archive);
            LEASES.put(fileSystem, archive);
            return fileSystem;
        }
    }

    public static void release(final FileSystem fileSystem) throws IOException {
        synchronized (ARCHIVES) {
            final Archive archive = LEASES.get(fileSystem);
            if (archive != null) {
                if (--archive.references != 0) return;
                LEASES.remove(fileSystem);
                ARCHIVES.remove(archive.key, archive);
            }
            fileSystem.close();
        }
    }

    private static ArchiveKey archiveKey(final Path source) {
        if (source.getFileSystem() != FileSystems.getDefault()) return null;
        final Path absolute = source.toAbsolutePath();
        // In a path containing a symlink, collapsing '..' can refer to a different archive.
        if (!absolute.equals(absolute.normalize())) return null;
        try {
            final BasicFileAttributes attributes = Files.readAttributes(absolute, BasicFileAttributes.class);
            return attributes.isRegularFile() ? new ArchiveKey(absolute, attributes.size(), attributes.lastModifiedTime(),
                attributes.creationTime(), attributes.fileKey()) : null;
        } catch (IOException failure) {
            return null;
        }
    }

    private record ArchiveKey(Path path, long size, FileTime modified, FileTime created, Object fileKey) {
    }

    private static final class Archive {
        private final ArchiveKey key;
        private final FileSystem fileSystem;
        private int references = 1;

        private Archive(final ArchiveKey key, final FileSystem fileSystem) {
            this.key = key;
            this.fileSystem = fileSystem;
        }
    }
}
