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
    private static final String ASSETS = "assets/";

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
                    if (attributes.isSymbolicLink() || attributes.isOther()) {
                        uncertainPrefixes.add(key(directory));
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    paths.add(key(directory));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFile(final @NonNull Path file, final @NonNull BasicFileAttributes attributes) {
                    if (attributes.isSymbolicLink() || attributes.isOther()) {
                        // A linked directory may expose descendants outside the walked tree.
                        uncertainPrefixes.add(key(file));
                    } else {
                        paths.add(key(file));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFileFailed(final @NonNull Path file, final @NonNull IOException failure) {
                    uncertainPrefixes.add(key(file));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult postVisitDirectory(final @NonNull Path directory, final IOException failure) {
                    if (failure != null) {
                        uncertainPrefixes.add(key(directory));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final NoSuchFileException ignored) {
            // A missing root is an empty snapshot, under the generation's existing stability contract.
        } catch (final IOException | SecurityException failure) {
            this.uncertainPrefixes.add(ASSETS);
        }
    }

    private String key(final Path path) {
        final String absolute = path.toString();
        return absolute.length() == rootPath.length() ? ASSETS
            : ASSETS + absolute.substring(prefix.length()).replace(File.separatorChar, '/');
    }

    private static boolean isSimpleResourceName(final String name) {
        int start = ASSETS.length();
        for (int end = start; end <= name.length(); end++) {
            if (end < name.length()) {
                final char value = name.charAt(end);
                if (value == '\\' || CASE_INSENSITIVE && (value == '~' || value == ':')) return false;
                if (value != '/') continue;
            }
            final int length = end - start;
            if (length == 0 && !(start == ASSETS.length() && end == name.length())
                || length == 1 && name.charAt(start) == '.'
                || length == 2 && name.charAt(start) == '.' && name.charAt(start + 1) == '.') return false;
            if (CASE_INSENSITIVE && length > 0) {
                final char last = name.charAt(end - 1);
                if (last == '.' || last == ' ') return false;
            }
            start = end + 1;
        }
        return true;
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
        return lookup(path.length() == rootPath.length() ? ASSETS
            : ASSETS + path.substring(prefix.length()).replace(File.separatorChar, '/'));
    }

    /** BASE strips every literal assets/ occurrence; only the ordinary leading prefix is indexed directly. */
    Boolean containsResource(final String name) {
        if (!name.startsWith(ASSETS) || name.indexOf(ASSETS, ASSETS.length()) >= 0 || !isSimpleResourceName(name)) return null;
        return lookup(name);
    }

    private Boolean lookup(final String name) {
        if (this.paths.contains(name)) {
            return Boolean.TRUE;
        }
        for (final String uncertain : this.uncertainPrefixes) {
            if (name.regionMatches(CASE_INSENSITIVE, 0, uncertain, 0, uncertain.length())
                && (name.length() == uncertain.length()
                || uncertain.endsWith("/")
                || name.charAt(uncertain.length()) == '/')) {
                return null;
            }
        }
        return Boolean.FALSE;
    }
}
