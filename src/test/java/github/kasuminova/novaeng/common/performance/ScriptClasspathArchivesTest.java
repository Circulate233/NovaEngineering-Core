package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScriptClasspathArchivesTest {
    @TempDir
    Path directory;

    @Test
    void twoProvidersCanReadUntilBothReleaseTheSharedArchive() throws IOException {
        final Path jar = directory.resolve("scripts.jar");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            zip.putNextEntry(new ZipEntry("test/Example.class"));
            zip.write(42);
            zip.closeEntry();
        }

        final FileSystem first = ScriptClasspathArchives.open(jar, null);
        final FileSystem second = ScriptClasspathArchives.open(jar, null);
        try {
            assertSame(first, second);
            ScriptClasspathArchives.release(first);
            assertEquals(42, Files.readAllBytes(second.getPath("test/Example.class"))[0]);
        } finally {
            ScriptClasspathArchives.release(second);
        }
        assertFalse(second.isOpen());

        final FileSystem reopened = ScriptClasspathArchives.open(jar, null);
        try {
            assertNotSame(second, reopened);
        } finally {
            ScriptClasspathArchives.release(reopened);
        }
    }
}
