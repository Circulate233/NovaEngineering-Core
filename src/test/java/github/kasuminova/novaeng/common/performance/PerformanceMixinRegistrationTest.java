package github.kasuminova.novaeng.common.performance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceMixinRegistrationTest {

    private static final List<String> MIXINS = List.of(
        "ae2.MixinBetterMeteoriteCompassMetadata",
        "ae2.MixinCompassReaderRegions",
        "ae2.MixinMeteorDataNameEncoder",
        "ae2.MixinSpawnDataInput",
        "ar.MixinAtmosphereDimensionMap",
        "ar.MixinAtmosphereHandler",
        "ar.MixinAtmosphereTemperature",
        "ar.MixinAtmosphereTemps",
        "ar.MixinAtmosphereTypes",
        "cofh.MixinWeightedBlockPredicate",
        "cofh.MixinWorldGenMinableClusterGeometry",
        "cofh.MixinWorldGenPredicate",
        "cofh.MixinWorldGenSinglePosition",
        "ic2.MixinConfigTickPath",
        "minecraft.MixinAttributeModifierSnapshot",
        "minecraft.MixinBlockStoneMetadata",
        "minecraft.MixinGenLayerZoomRandom",
        "minecraft.MixinMinecraftServerDimensionIds",
        "minecraft.MixinWorldEntitySpawner",
        "minecraft.MixinWorldGenMinableGeometry",
        "minecraft.forge.AccessorDimensionManagerWorlds",
        "minecraft.forge.MixinDimensionManagerLeakCheck",
        "stellarcore.MixinPlayerChunkMapSnapshot",
        "zenutils.MixinCatenationManager"
    );

    private static void verifyRegistrations(final JsonObject configuration) {
        for (final String mixin : MIXINS) {
            assertEquals(1, occurrences(configuration.getAsJsonArray("mixins"), mixin), mixin);
            assertEquals(0, occurrences(configuration.getAsJsonArray("client"), mixin), mixin);
            assertEquals(0, occurrences(configuration.getAsJsonArray("server"), mixin), mixin);
        }
    }

    private static long occurrences(final JsonArray entries, final String mixin) {
        return entries == null ? 0 : StreamSupport.stream(entries.spliterator(), false)
                                                  .filter(entry -> entry.getAsString().equals(mixin)).count();
    }

    private static JsonObject productionConfiguration() throws IOException {
        try (final var stream = PerformanceMixinRegistrationTest.class.getClassLoader().getResourceAsStream("novaeng_core.mixin.json")) {
            assertNotNull(stream, "Production Mixin configuration is missing");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void everyPerformanceMixinIsRegisteredOnceInTheProductionConfiguration() throws IOException {
        final JsonObject configuration = productionConfiguration();
        verifyRegistrations(configuration);
        final String root = configuration.get("package").getAsString().replace('.', '/') + '/';
        for (final String mixin : MIXINS) {
            try (final var stream = this.getClass().getClassLoader().getResourceAsStream(root + mixin.replace('.', '/') + ".class")) {
                assertNotNull(stream, "Registered Mixin class is missing: " + mixin);
                final ClassNode type = new ClassNode();
                new ClassReader(stream).accept(type, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                assertTrue(type.invisibleAnnotations != null && type.invisibleAnnotations.stream()
                                                                                         .anyMatch(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")), mixin);
            }
        }
    }

    @Test
    void missingProductionRegistrationCannotBeHiddenByTheIsolatedMixinFixture() throws IOException {
        final JsonObject configuration = productionConfiguration();
        final JsonArray entries = configuration.getAsJsonArray("mixins");
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getAsString().equals(MIXINS.getFirst())) {
                entries.remove(i);
                break;
            }
        }
        assertThrows(AssertionError.class, () -> verifyRegistrations(configuration));
    }
}
