package github.kasuminova.novaeng.client.resource;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

/**
 * One read-only directory listing for the same model-reload lifetime as the existence cache.
 */
final class DirectoryResourceSnapshot {

    private static final boolean CASE_INSENSITIVE = File.separatorChar == '\\';

    private final String rootPath;
    private final String prefix;
    private final Set<String> paths = CASE_INSENSITIVE
        ? new ObjectRBTreeSet<>(String.CASE_INSENSITIVE_ORDER) : new ObjectOpenHashSet<>();
    private final Set<String> uncertainPrefixes = CASE_INSENSITIVE
        ? new ObjectRBTreeSet<>(String.CASE_INSENSITIVE_ORDER) : new ObjectOpenHashSet<>();

    DirectoryResourceSnapshot(final File root) {
        this.rootPath = root.getAbsolutePath();
        this.prefix = this.rootPath.endsWith(File.separator) ? this.rootPath : this.rootPath + File.separator;
        try {
            Files.walkFileTree(root.toPath().toAbsolutePath(), new SimpleFileVisitor<>() {
                @Override
                public @NonNull FileVisitResult preVisitDirectory(final @NonNull Path directory, final @NonNull BasicFileAttributes attributes) {
                    paths.add(directory.toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFile(final @NonNull Path file, final @NonNull BasicFileAttributes attributes) {
                    if (attributes.isSymbolicLink() || attributes.isOther()) {
                        // A linked directory may expose descendants outside the walked tree.
                        uncertainPrefixes.add(file.toString());
                    } else {
                        paths.add(file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFileFailed(final @NonNull Path file, final @NonNull IOException failure) {
                    uncertainPrefixes.add(file.toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult postVisitDirectory(final @NonNull Path directory, final IOException failure) {
                    if (failure != null) {
                        uncertainPrefixes.add(directory.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final NoSuchFileException ignored) {
            // A missing root is an empty snapshot, under the generation's existing stability contract.
        } catch (final IOException | SecurityException failure) {
            this.uncertainPrefixes.add(this.rootPath);
        }
    }

    private static boolean isSimplePath(final String path) {
        int start = 0;
        for (int end = 0; end <= path.length(); end++) {
            if (end < path.length()) {
                final char value = path.charAt(end);
                if (CASE_INSENSITIVE && (value == '~' || value == ':' && end != 1)) {
                    return false;
                }
                if (value != File.separatorChar) {
                    continue;
                }
            }
            final int length = end - start;
            if (length == 1 && path.charAt(start) == '.'
                || length == 2 && path.charAt(start) == '.' && path.charAt(start + 1) == '.') {
                return false;
            }
            if (CASE_INSENSITIVE && length > 0) {
                final char last = path.charAt(end - 1);
                if (last == '.' || last == ' ') {
                    return false;
                }
            }
            start = end + 1;
        }
        return true;
    }

    /**
     * Returns null when a path needs the original filesystem probe.
     */
    Boolean contains(final String path) {
        if (!(path.length() == this.rootPath.length()
            && path.regionMatches(CASE_INSENSITIVE, 0, this.rootPath, 0, this.rootPath.length()))
            && !path.regionMatches(CASE_INSENSITIVE, 0, this.prefix, 0, this.prefix.length())) {
            return null;
        }
        // Do not normalize through symlinks or give traversal/alternate-stream paths a new interpretation.
        if (!isSimplePath(path)) {
            return null;
        }
        if (this.paths.contains(path)) {
            return Boolean.TRUE;
        }
        for (final String uncertain : this.uncertainPrefixes) {
            if (path.regionMatches(CASE_INSENSITIVE, 0, uncertain, 0, uncertain.length())
                && (path.length() == uncertain.length()
                || uncertain.endsWith(File.separator)
                || path.charAt(uncertain.length()) == File.separatorChar)) {
                return null;
            }
        }
        return Boolean.FALSE;
    }
}
