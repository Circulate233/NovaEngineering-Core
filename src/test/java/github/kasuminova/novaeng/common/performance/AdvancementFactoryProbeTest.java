package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancementFactoryProbeTest {
    private static final String ROOT = "assets/example/advancements/";
    private static final String FACTORIES = ROOT + "_factories.json";
    @TempDir
    Path directory;

    private Path archive(String name, String... paths) throws IOException {
        Path jar = directory.resolve(name);
        try (var zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (String path : paths) {
                zip.putNextEntry(new ZipEntry(path));
                zip.closeEntry();
            }
        }
        return jar;
    }

    @Test
    void absentFactoriesDoNotMaskReplacementsOrOtherNamespaces() throws IOException {
        var jar = archive("both.jar", ROOT + "a.json", "assets/other/advancements/_factories.json");
        assertFalse(ArchiveResourceIndex.mayContain(jar.toFile(), FACTORIES));
        assertTrue(ArchiveResourceIndex.mayContain(jar.toFile(), "assets/other/advancements/_factories.json"));
        archive("both.jar", FACTORIES);
        assertTrue(ArchiveResourceIndex.mayContain(jar.toFile(), FACTORIES));
    }

    @Test
    void directoriesAndUnconventionalArchivesFallBackToForge() throws IOException {
        assertTrue(ArchiveResourceIndex.mayContain(directory.toFile(), FACTORIES));
        assertTrue(ArchiveResourceIndex.mayContain(archive("odd.jar", "../bad").toFile(), FACTORIES));
        assertTrue(ArchiveResourceIndex.mayContain(archive("dir.jar", FACTORIES + "/child").toFile(), FACTORIES));
    }
}
