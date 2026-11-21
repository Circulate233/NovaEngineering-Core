package github.kasuminova.novaeng.common.performance;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class DirectoryTreeScan {
    private DirectoryTreeScan() {
    }

    public static void scan(final File directory, final BooleanSupplier current,
                            final Consumer<String> paths) throws IOException {
        if (!current.getAsBoolean() || !directory.exists()) {
            return;
        }
        final Path root = directory.toPath();
        if (!Files.isDirectory(root)) {
            throw new IOException("Directory index root is not a directory: " + directory.getAbsolutePath());
        }
        // The visitor receives attributes from enumeration; do not stat every entry twice via File.isDirectory/isFile.
        Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(final @NotNull Path dir, final @NotNull BasicFileAttributes attributes) {
                return current.getAsBoolean() ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
            }

            @Override
            public @NotNull FileVisitResult visitFile(final @NotNull Path file, final @NotNull BasicFileAttributes attributes) {
                if (!current.getAsBoolean()) {
                    return FileVisitResult.TERMINATE;
                }
                if (attributes.isRegularFile()) {
                    paths.accept(root.relativize(file).toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
