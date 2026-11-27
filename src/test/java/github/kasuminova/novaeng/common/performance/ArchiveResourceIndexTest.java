package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveResourceIndexTest {
    @TempDir
    Path directory;

    private Path archive(final String name, final List<String> entries) throws IOException {
        final Path path = this.directory.resolve(name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
            for (int index = 0; index < entries.size(); index++) {
                zip.putNextEntry(new ZipEntry(entries.get(index)));
                zip.closeEntry();
            }
        }
        return path;
    }

    @Test
    void implicitRootsExactEntriesAndDescendantsKeepZipfsPresenceSemantics() throws IOException {
        final Path jar = this.archive("roots.jar", List.of(
            "assets/test/recipes/foo-variant", "assets/test/recipes/foo.json",
            "assets/test/recipes/foo/child", "assets/test/recipes/foo0/child",
            "assets/other/advancements/", "assets/test/recipes/last/"));
        final List<String> expectedPaths = List.of("assets/test/recipes", "assets/test/recipes/",
            "assets/test/recipes/foo", "assets/test/recipes/foo/", "assets/test/recipes/foo.json",
            "assets/test/recipes/last", "assets/other/advancements");
        for (int index = 0; index < expectedPaths.size(); index++) {
            final String path = expectedPaths.get(index);
            assertTrue(ArchiveResourceIndex.mayContain(jar.toFile(), path), path);
        }
        final List<String> absentPaths = List.of("assets/test/recipes/foo.", "assets/test/recipes/fo",
            "assets/test/recipes/fooz", "assets/test/recipes/zzz", "assets/test/advancements");
        for (int index = 0; index < absentPaths.size(); index++) {
            final String path = absentPaths.get(index);
            assertFalse(ArchiveResourceIndex.mayContain(jar.toFile(), path), path);
        }
    }

    @Test
    void generatedNamesMatchExactOrDirectoryPrefixOracle() throws IOException {
        final Random random = new Random(20261119);
        final List<String> entries = new ObjectArrayList<>();
        for (int i = 0; i < 600; i++) {
            entries.add("assets/test/recipes/" + i + "-" + random.nextInt(20) + "/child.json");
        }
        entries.add("assets/test/recipes/sibling-variant");
        entries.add("assets/test/recipes/sibling.child");
        entries.add("assets/test/recipes/sibling/child");
        final Path jar = this.archive("generated.jar", entries);
        for (int i = 0; i < 2000; i++) {
            final String path = "assets/test/recipes/" + random.nextInt(700) + "-" + random.nextInt(20);
            final boolean expected = entries.stream().anyMatch(entry -> entry.equals(path) || entry.startsWith(path + '/'));
            assertEquals(expected, ArchiveResourceIndex.mayContain(jar.toFile(), path), path);
        }
        assertTrue(ArchiveResourceIndex.mayContain(jar.toFile(), "assets/test/recipes/sibling"));
        assertFalse(ArchiveResourceIndex.mayContain(jar.toFile(), "assets/test/recipes/sibling."));
    }

    @Test
    void replacementDeletionAndUncertainProvidersStillUseFreshMetadata() throws IOException {
        final String path = "assets/test/recipes/old.json";
        final Path jar = this.archive("replace.jar", List.of(path));
        final FileTime modified = Files.getLastModifiedTime(jar);
        assertTrue(ArchiveResourceIndex.mayContain(jar.toFile(), path));
        this.archive("replace.jar", List.of("assets/test/recipes/new.json"));
        Files.setLastModifiedTime(jar, FileTime.fromMillis(modified.toMillis() + 2000));
        assertFalse(ArchiveResourceIndex.mayContain(jar.toFile(), path));
        assertTrue(ArchiveResourceIndex.mayContain(jar.toFile(), "assets/test/recipes/new.json"));
        Files.delete(jar);
        assertTrue(ArchiveResourceIndex.mayContain(jar.toFile(), path));
        assertTrue(ArchiveResourceIndex.mayContain(this.directory.toFile(), path));
        final Path odd = this.archive("odd.jar", List.of("../unusual", path));
        assertTrue(ArchiveResourceIndex.mayContain(odd.toFile(), "assets/test/recipes/missing.json"));
        final Path ordinary = this.archive("ordinary.jar", List.of(path, "assets/test/models/present.json"));
        assertTrue(ArchiveResourceIndex.mayContain(ordinary.toFile(), "assets/test/recipes/../missing.json"));
        assertTrue(ArchiveResourceIndex.mayContain(ordinary.toFile(), "assets/test/recipes//missing.json"));
        assertTrue(ArchiveResourceIndex.mayContain(ordinary.toFile(), "assets/test/models/missing.json"));
    }

    @Test
    void waypointIconProbeUsesOnlyCurrentConventionalSnapshots() throws IOException {
        final Path jar = this.archive("waypoints.jar", List.of("assets/test/textures/waypoint/icons/near.png",
            "assets/test/textures/waypoint/icon/"));
        assertTrue(ArchiveResourceIndex.mayContainWaypointIcons(jar.toFile()));
        ArchiveResourceIndex.mayContain(jar.toFile(), "assets/test/recipes");
        assertFalse(ArchiveResourceIndex.mayContainWaypointIcons(jar.toFile()));

        final FileTime modified = Files.getLastModifiedTime(jar);
        this.archive("waypoints.jar", List.of("assets/test/textures/waypoint/icon/actual.PNG"));
        Files.setLastModifiedTime(jar, FileTime.fromMillis(modified.toMillis() + 2000));
        assertTrue(ArchiveResourceIndex.mayContainWaypointIcons(jar.toFile()));
        ArchiveResourceIndex.mayContain(jar.toFile(), "assets/test/recipes");
        assertTrue(ArchiveResourceIndex.mayContainWaypointIcons(jar.toFile()));
    }
}
