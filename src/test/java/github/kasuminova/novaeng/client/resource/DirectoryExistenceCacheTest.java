package github.kasuminova.novaeng.client.resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DirectoryExistenceCacheTest {
    @TempDir
    Path temporary;
    private final DirectoryExistenceCache cache = DirectoryExistenceCache.instance();

    @AfterEach
    void closeGeneration() {
        while (cache.active() != null) cache.end();
    }

    private File root() throws IOException {
        final Path root = Files.createDirectories(temporary.resolve("pack"));
        Files.createDirectories(root.resolve("namespace/textures"));
        Files.writeString(root.resolve("namespace/textures/present.txt"), "present");
        return root.toFile();
    }

    private static boolean original(final File root, final String name) {
        return new File(root, name.replace("assets/", "")).exists();
    }

    @Test
    void logicalNamesAndLegacyFileQueriesUseTheSameSnapshot() throws IOException {
        final File root = root();
        final DirectoryResourceSnapshot snapshot = new DirectoryResourceSnapshot(root);
        assertEquals(Boolean.TRUE, snapshot.containsResource("assets/namespace/textures/present.txt"));
        assertEquals(Boolean.TRUE, snapshot.containsResource("assets/namespace/textures"));
        assertEquals(Boolean.TRUE, snapshot.containsResource("assets/"));
        assertEquals(Boolean.FALSE, snapshot.containsResource("assets/namespace/textures/missing.txt"));
        assertEquals(snapshot.containsResource("assets/namespace/textures/present.txt"),
            snapshot.contains(new File(root, "namespace/textures/present.txt").getAbsolutePath()));
        assertEquals(Boolean.TRUE, snapshot.contains(root.getAbsolutePath()));
        assertNull(snapshot.contains(temporary.resolve("elsewhere.txt").toString()));
        assertEquals(File.separatorChar == '\\', snapshot.containsResource("assets/NAMESPACE/TEXTURES/PRESENT.TXT"));
    }

    @Test
    void unusualNamesKeepBasesGlobalReplacementAndFileResolution() throws IOException {
        final File root = root();
        Files.writeString(temporary.resolve("outside.txt"), "outside");
        Files.writeString(root.toPath().resolve("pack.mcmeta"), "metadata");
        cache.begin();
        for (final String name : new String[]{"", "assets", "pack.mcmeta", "ASSETS/namespace/textures/present.txt",
            "assets/assets/namespace/textures/present.txt", "assets/namespace/assets/textures/present.txt",
            "assets/namespace/./textures/present.txt", "assets/namespace/textures/../textures/present.txt",
            "assets/namespace//textures/present.txt", "assets/namespace/textures/", "assets/../outside.txt",
            "assets/namespace\\textures\\present.txt", "assets/namespace/textures/present.txt.",
            "assets/namespace/textures/present.txt ", "assets/namespace/textures/PRESEN~1.TXT",
            "assets/namespace/textures/present.txt:stream"}) {
            assertEquals(original(root, name), cache.existsResource(root, name), name);
        }
        assertTrue(cache.existsResource(root, "assets/namespace/assets/textures/present.txt"));
        assertTrue(cache.existsResource(root, "assets/../outside.txt"));
    }

    @Test
    void normalAnswersStayInsideTheReloadAndDoNotCreatePerNameCacheEntries() throws IOException {
        final File root = root();
        final Path present = root.toPath().resolve("namespace/textures/present.txt");
        final Path added = root.toPath().resolve("namespace/textures/added.txt");
        cache.begin();
        assertTrue(cache.existsResource(root, "assets/namespace/textures/present.txt"));
        assertFalse(cache.existsResource(root, "assets/namespace/textures/added.txt"));
        assertTrue(cache.active().isEmpty());
        Files.delete(present);
        Files.writeString(added, "added");
        cache.begin();
        assertTrue(cache.existsResource(root, "assets/namespace/textures/present.txt"));
        assertFalse(cache.existsResource(root, "assets/namespace/textures/added.txt"));
        cache.end();
        assertTrue(cache.existsResource(root, "assets/namespace/textures/present.txt"));
        cache.end();
        assertNull(cache.active());
        assertFalse(cache.existsResource(root, "assets/namespace/textures/present.txt"));
        assertTrue(cache.existsResource(root, "assets/namespace/textures/added.txt"));
        cache.begin();
        assertFalse(cache.existsResource(root, "assets/namespace/textures/present.txt"));
        assertTrue(cache.existsResource(root, "assets/namespace/textures/added.txt"));
    }

    @Test
    void caseEquivalentWindowsRootsShareTheGenerationSnapshot() throws IOException {
        assumeTrue(File.separatorChar == '\\');
        final File root = root();
        cache.begin();
        assertTrue(cache.existsResource(root, "assets/namespace/textures/present.txt"));
        Files.delete(root.toPath().resolve("namespace/textures/present.txt"));
        final File sameRoot = new File(root.getPath().toUpperCase(Locale.ROOT));
        assertTrue(cache.existsResource(sameRoot, "assets/namespace/textures/present.txt"));
        cache.end();
        assertFalse(cache.existsResource(sameRoot, "assets/namespace/textures/present.txt"));
    }

    @Test
    void uncertainLinkPathsKeepTheFilesystemFallback() throws IOException {
        final File root = root();
        final Path target = Files.createDirectories(temporary.resolve("link-target"));
        Files.writeString(target.resolve("linked.txt"), "linked");
        final Path link = root.toPath().resolve("link");
        try {
            Files.createSymbolicLink(link, target);
        } catch (final IOException | UnsupportedOperationException | SecurityException failure) {
            assumeTrue(false, "Symbolic links unavailable: " + failure.getClass().getSimpleName());
        }
        final DirectoryResourceSnapshot snapshot = new DirectoryResourceSnapshot(root);
        assertNull(snapshot.containsResource("assets/link"));
        assertNull(snapshot.containsResource("assets/link/linked.txt"));
        assertEquals(Boolean.FALSE, snapshot.containsResource("assets/linkish/linked.txt"));
        cache.begin();
        assertEquals(original(root, "assets/link/linked.txt"), cache.existsResource(root, "assets/link/linked.txt"));
        assertTrue(cache.active().containsKey(new File(root, "link/linked.txt").getAbsolutePath()));
    }

    @Test
    void aMissingRootRemainsEmptyUntilTheNextGeneration() throws IOException {
        final File root = temporary.resolve("not-yet-created").toFile();
        cache.begin();
        assertFalse(cache.existsResource(root, "assets/namespace/new.txt"));
        Files.createDirectories(root.toPath().resolve("namespace"));
        Files.writeString(root.toPath().resolve("namespace/new.txt"), "new");
        assertFalse(cache.existsResource(root, "assets/namespace/new.txt"));
        cache.end();
        assertTrue(cache.existsResource(root, "assets/namespace/new.txt"));
    }
}
