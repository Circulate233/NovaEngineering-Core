package github.kasuminova.novaeng.common.performance;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

/**
 * Retained entry point for Forge recipe discovery; all mod data roots share the archive snapshot.
 */
public final class RecipeJarProbe {
    private RecipeJarProbe() {
    }

    public static boolean mayContain(final File source, final String base) {
        return ArchiveResourceIndex.mayContain(source, base);
    }

    public static boolean isConventional(final File source, final ZipFile zip) throws IOException {
        return ArchiveResourceIndex.isConventional(source, zip);
    }
}
