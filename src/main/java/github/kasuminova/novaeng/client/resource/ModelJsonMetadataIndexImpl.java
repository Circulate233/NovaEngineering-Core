package github.kasuminova.novaeng.client.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import github.kasuminova.novaeng.NovaEngineeringCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Generation-scoped JSON metadata index keyed by resource location and source-pack identity.
 *
 * <p>Each query first resolves and opens the current winning source. Static sources then share one
 * parsed result, while unknown and mutable sources are parsed every time. The cache therefore
 * avoids duplicate JSON parsing without bypassing dynamic overlay lookup or priority semantics.</p>
 */
public final class ModelJsonMetadataIndexImpl implements ModelJsonMetadataIndex {

    private static final Metadata EMPTY = new Metadata(null, false);
    private static final ModelJsonMetadataIndex INSTANCE = new ModelJsonMetadataIndexImpl();

    private final AtomicLong nextGeneration = new AtomicLong();
    private final AtomicReference<Generation> activeGeneration = new AtomicReference<>();

    private ModelJsonMetadataIndexImpl() {
    }

    /**
     * Returns the shared metadata owner used by resource and model-loader mixins.
     *
     * @return shared metadata index
     */
    public static ModelJsonMetadataIndex instance() {
        return INSTANCE;
    }

    @Override
    public void begin() {
        final Generation generation = new Generation(this.nextGeneration.incrementAndGet());
        if (!this.activeGeneration.compareAndSet(null, generation)) {
            final IllegalStateException failure = new IllegalStateException(
                "A model metadata generation is already active");
            NovaEngineeringCore.log.error("Failed to begin a nested model metadata generation.", failure);
            throw failure;
        }
    }

    @Override
    public boolean isActive() {
        return this.activeGeneration.get() != null;
    }

    @Override
    public boolean isLibNineType(final ResourceLocation location, final String type) {
        final Metadata metadata = this.lookup(location);
        return metadata.libNineType != null && metadata.libNineType.equals(type);
    }

    @Override
    public boolean hasUvlMarker(final ResourceLocation location) {
        return this.lookup(location).uvlMarker;
    }

    @Override
    public void end() {
        final Generation generation = this.activeGeneration.getAndSet(null);
        if (generation == null) {
            final IllegalStateException failure = new IllegalStateException(
                "No model metadata generation is active");
            NovaEngineeringCore.log.error("Failed to end a model metadata generation.", failure);
            throw failure;
        }
        generation.metadata.clear();
        generation.loggedFailures.clear();
    }

    private Metadata lookup(final ResourceLocation location) {
        final Generation generation = this.activeGeneration.get();
        if (generation == null) {
            return EMPTY;
        }

        try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location)) {
            final InputStream stream = resource.getInputStream();
            if (!(stream instanceof SourcedResourceStream sourced) || !sourced.isCacheableSource()) {
                return parse(stream, location, generation);
            }

            final MetadataKey key = new MetadataKey(location, sourced.sourceIdentity(), generation.id);
            final Metadata result = generation.metadata.computeIfAbsent(
                key, ignored -> parse(stream, location, generation));
            if (this.activeGeneration.get() != generation) {
                generation.metadata.remove(key, result);
                return EMPTY;
            }
            return result;
        } catch (final FileNotFoundException ignored) {
            return EMPTY;
        } catch (final IOException | RuntimeException failure) {
            logFailure(generation, location, failure);
            return EMPTY;
        }
    }

    private static Metadata parse(final InputStream stream,
                                  final ResourceLocation location,
                                  final Generation generation) {
        final JsonObject json;
        try {
            final JsonElement root = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (root == null || !root.isJsonObject()) {
                logFailure(generation, location,
                    new IllegalStateException("Model JSON root is not an object"));
                return EMPTY;
            }
            json = root.getAsJsonObject();
        } catch (final RuntimeException failure) {
            logFailure(generation, location, failure);
            return EMPTY;
        }

        String libNineType = null;
        boolean uvlMarker = false;
        try {
            if (json.has("9s")) {
                libNineType = json.get("9s").getAsString();
            }
        } catch (final RuntimeException failure) {
            logFailure(generation, location, failure);
        }
        try {
            if (json.has("ae2_uvl_marker")) {
                uvlMarker = json.get("ae2_uvl_marker").getAsBoolean();
            }
        } catch (final RuntimeException failure) {
            logFailure(generation, location, failure);
        }
        return new Metadata(libNineType, uvlMarker);
    }

    private static void logFailure(final Generation generation,
                                   final ResourceLocation location,
                                   final Throwable failure) {
        if (generation.loggedFailures.add(location)) {
            NovaEngineeringCore.log.warn(
                "Failed to read model JSON metadata for {} in generation {}.",
                location, generation.id, failure);
        }
    }

    private static final class Generation {
        private final long id;
        private final ConcurrentHashMap<MetadataKey, Metadata> metadata = new ConcurrentHashMap<>();
        private final Set<ResourceLocation> loggedFailures = ConcurrentHashMap.newKeySet();

        private Generation(final long id) {
            this.id = id;
        }
    }

    private record MetadataKey(ResourceLocation location, Object sourceIdentity, long generation) {

        @Override
            public boolean equals(final Object other) {
                if (this == other) {
                    return true;
                }
                if (!(other instanceof MetadataKey(ResourceLocation location1, Object identity, long generation1))) {
                    return false;
                }
                return this.generation == generation1
                    && this.sourceIdentity == identity
                    && this.location.equals(location1);
            }

            @Override
            public int hashCode() {
                int result = this.location.hashCode();
                result = 31 * result + System.identityHashCode(this.sourceIdentity);
                result = 31 * result + Long.hashCode(this.generation);
                return result;
            }
        }

    private record Metadata(String libNineType, boolean uvlMarker) {
    }
}
