package github.kasuminova.novaeng.client.resource;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import github.kasuminova.novaeng.NovaEngineeringCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Generation-scoped JSON metadata index keyed by resource location and source-pack identity.
 *
 * <p>Each query first resolves and opens the current winning source. Static sources then share one
 * parsed result, while unknown and mutable sources are parsed every time. The cache therefore
 * avoids duplicate JSON parsing without bypassing dynamic overlay lookup or priority semantics.</p>
 *
 * <p>Nesting is tolerated through a depth counter rather than being rejected. A reload started from inside
 * another reload would otherwise abort the outer reload with an exception, which is a far worse outcome than
 * serving a slightly longer lived answer.</p>
 */
public final class ModelJsonMetadataIndexImpl implements ModelJsonMetadataIndex {

    private static final Metadata EMPTY = new Metadata(null, false);
    private static final ModelJsonMetadataIndex INSTANCE = new ModelJsonMetadataIndexImpl();

    private final AtomicLong nextGeneration = new AtomicLong();
    private final AtomicInteger depth = new AtomicInteger();
    private final AtomicReference<Generation> activeGeneration = new AtomicReference<>();

    private final Supplier<IResourceManager> resources;

    private ModelJsonMetadataIndexImpl() {
        this(() -> Minecraft.getMinecraft().getResourceManager());
    }

    ModelJsonMetadataIndexImpl(final Supplier<IResourceManager> resources) {
        this.resources = resources;
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
        if (this.depth.incrementAndGet() == 1) {
            this.activeGeneration.set(new Generation(this.nextGeneration.incrementAndGet()));
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
        if (this.depth.updateAndGet(current -> current > 0 ? current - 1 : 0) != 0) {
            return;
        }
        final Generation generation = this.activeGeneration.getAndSet(null);
        if (generation != null) {
            generation.metadata.clear();
            generation.loggedFailures.clear();
        }
    }

    private static Metadata parse(final InputStream stream,
                                  final ResourceLocation location,
                                  final Generation generation) {
        try (JsonReader reader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return readMetadata(reader);
        } catch (final IOException | RuntimeException failure) {
            logFailure(generation, location, failure);
            return EMPTY;
        }
    }

    @SuppressWarnings("deprecation")
    static Metadata readMetadata(final JsonReader reader) throws IOException {
        String libNineType = null;
        boolean uvlMarker = false;
        boolean validUvlMarker = true;
        reader.setLenient(true);
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new IOException("Model JSON root is not an object");
        }
        reader.beginObject();
        while (reader.hasNext()) {
            final String name = reader.nextName();
            if ("9s".equals(name)) {
                libNineType = readLibNineType(reader);
            } else if ("ae2_uvl_marker".equals(name)) {
                final JsonToken token = reader.peek();
                if (token == JsonToken.BOOLEAN) {
                    uvlMarker = reader.nextBoolean();
                } else if (token == JsonToken.STRING) {
                    uvlMarker = Boolean.parseBoolean(reader.nextString());
                } else if (token == JsonToken.NULL) {
                    reader.nextNull();
                } else {
                    reader.skipValue();
                    validUvlMarker = false;
                }
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        reader.setLenient(false);
        if (reader.peek() != JsonToken.END_DOCUMENT) {
            throw new IOException("Model JSON has trailing content");
        }
        return new Metadata(libNineType, validUvlMarker && uvlMarker);
    }

    private static String readLibNineType(final JsonReader reader) throws IOException {
        final JsonToken token = reader.peek();
        if (token == JsonToken.STRING || token == JsonToken.NUMBER) {
            return reader.nextString();
        }
        if (token == JsonToken.BOOLEAN) {
            return Boolean.toString(reader.nextBoolean());
        }
        if (token == JsonToken.BEGIN_ARRAY) {
            reader.beginArray();
            String value = reader.hasNext() ? readLibNineType(reader) : null;
            while (reader.hasNext()) {
                reader.skipValue();
                value = null;
            }
            reader.endArray();
            return value;
        }
        reader.skipValue();
        return null;
    }

    private Metadata lookup(final ResourceLocation location) {
        final Generation generation = this.activeGeneration.get();
        if (generation == null) {
            return EMPTY;
        }

        try (IResource resource = this.resources.get().getResource(location)) {
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

    record Metadata(String libNineType, boolean uvlMarker) {
    }
}
