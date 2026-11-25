package github.kasuminova.novaeng.common.util;

import com.cleanroommc.discovery.CleanroomModDiscoverer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.Launch;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

final class PulsarBackportSelection {
    static final boolean PULSAR_0_6_2 = isPulsar062();
    static final boolean SCALAR = PULSAR_0_6_2 && usesScalarLighting();

    private PulsarBackportSelection() {
    }

    private static boolean isPulsar062() {
        // Read metadata without defining a Pulsar class during mixin selection.
        for (final var source : CleanroomModDiscoverer.instance().modSources("pulsar")) {
            try (final var archive = new ZipFile(source)) {
                final var entry = archive.getEntry("mcmod.info");
                if (entry == null) continue;
                try (final var reader = new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8)) {
                    final var metadata = JsonParser.parseReader(reader);
                    if (!metadata.isJsonArray()) continue;
                    for (final var mod : metadata.getAsJsonArray()) {
                        if (mod instanceof JsonObject data && data.has("modid") && data.has("version")
                            && "pulsar".equals(data.get("modid").getAsString())) {
                            return "0.6.2".equals(data.get("version").getAsString());
                        }
                    }
                }
            } catch (IOException | JsonParseException ignored) {
                // An unrecognized artifact cannot be assumed to have the 0.6.2 layout.
            }
        }
        return false;
    }

    private static boolean usesScalarLighting() {
        final Path home = Launch.minecraftHome == null ? Path.of(".") : Launch.minecraftHome.toPath();
        final Path config = home.resolve("config/pulsar.cfg");
        try {
            // Match Pulsar 0.6.2's restart-only selection. Unset mode defaults to SCALAR.
            return !Files.isRegularFile(config)
                || !Pattern.compile("(?m)^\\s*S:lightingMode=RGB\\s*$").matcher(Files.readString(config)).find();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read Pulsar lighting mode", e);
        }
    }
}
