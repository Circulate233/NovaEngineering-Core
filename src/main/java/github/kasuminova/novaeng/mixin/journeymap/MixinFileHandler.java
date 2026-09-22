package github.kasuminova.novaeng.mixin.journeymap;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import journeymap.client.io.FileHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Skips JourneyMap's theme resource copy while the source jar is unchanged.
 *
 * <p>{@code ThemeLoader} unpacks its theme directory out of the mod jar on every launch, which costs a full
 * stream-and-inflate pass over those resources even though nothing changed. A marker beside the destination
 * records the source jar's size and modification time; while both match, the copy is skipped.</p>
 *
 * <p>The marker is deliberately named outside the copied directory so it is never overwritten by the copy, and
 * deleting it forces the next launch to unpack again — the recovery path for an edited or deleted theme.</p>
 */
@Mixin(value = FileHandler.class, remap = false)
public abstract class MixinFileHandler {

    @Unique
    private static final String NOVA$MARKER_PREFIX = ".novaeng-copy-marker-";

    @WrapMethod(method = "copyResources(Ljava/io/File;Lnet/minecraft/util/ResourceLocation;Ljava/lang/String;Z)Z",
        remap = false, require = 1)
    private static boolean nova$skipUnchangedCopy(final File directory,
                                                  final ResourceLocation resourceLocation,
                                                  final String destDirName,
                                                  final boolean overwrite,
                                                  final Operation<Boolean> original) {
        if (!NovaEngCoreConfig.CLIENT.optimizeJourneymapThemeCopy || directory == null || destDirName == null) {
            return original.call(directory, resourceLocation, destDirName, overwrite);
        }

        final String fingerprint = nova$fingerprint(resourceLocation);
        if (fingerprint == null) {
            return original.call(directory, resourceLocation, destDirName, overwrite);
        }

        final File destination = new File(directory, destDirName);
        final File marker = new File(directory, NOVA$MARKER_PREFIX + destDirName);
        if (destination.isDirectory() && fingerprint.equals(nova$readMarker(marker))) {
            return true;
        }

        final boolean copied = original.call(directory, resourceLocation, destDirName, overwrite);
        if (copied) {
            nova$writeMarker(marker, fingerprint);
        }
        return copied;
    }

    /**
     * Describes the source the copy is taken from: the mod jar and its identity.
     *
     * @param resourceLocation resource being unpacked
     * @return a fingerprint string, or {@code null} when the source cannot be described
     */
    @Unique
    private static String nova$fingerprint(final ResourceLocation resourceLocation) {
        if (resourceLocation == null) {
            return null;
        }
        final ModContainer container = Loader.instance().getIndexedModList().get(resourceLocation.getNamespace());
        final File source = container == null ? null : container.getSource();
        if (source == null) {
            return null;
        }
        return resourceLocation.getNamespace() + '|' + source.getAbsolutePath() + '|' + source.length() + '|'
            + source.lastModified();
    }

    @Unique
    private static String nova$readMarker(final File marker) {
        try {
            return marker.isFile() ? Files.readString(marker.toPath(), StandardCharsets.UTF_8).trim() : null;
        } catch (final IOException | RuntimeException ignored) {
            return null;
        }
    }

    @Unique
    private static void nova$writeMarker(final File marker, final String fingerprint) {
        try {
            Files.writeString(marker.toPath(), fingerprint, StandardCharsets.UTF_8);
        } catch (final IOException | RuntimeException ignored) {
            // A marker that cannot be written only costs the next launch's copy.
        }
    }

}
