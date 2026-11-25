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
import java.util.function.Supplier;

/** Reload-scoped model metadata. The resource manager always selects the winning pack. */
public final class ModelJsonMetadataIndex {

    private static final Metadata EMPTY = new Metadata(false);
    private static final ModelJsonMetadataIndex INSTANCE = new ModelJsonMetadataIndex();

    private volatile Generation activeGeneration;
    private long nextGeneration;
    private int depth;
    private final ThreadLocal<Query> currentQuery = new ThreadLocal<>();
    private final Supplier<IResourceManager> resources;

    private ModelJsonMetadataIndex() {
        this(() -> Minecraft.getMinecraft().getResourceManager());
    }

    ModelJsonMetadataIndex(final Supplier<IResourceManager> resources) {
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

    private static Metadata parse(final InputStream stream,
                                  final ResourceLocation location,
                                  final Generation generation) {
        try {
            return readMetadata(new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        } catch (final IOException | RuntimeException failure) {
            logFailure(generation, location, failure);
            return null; // Failed reads must be retried, including reads from archive packs.
        }
    }

    @SuppressWarnings("deprecation")
    static Metadata readMetadata(final JsonReader reader) throws IOException {
        boolean uvlMarker = false;
        boolean validUvlMarker = true;
        reader.setLenient(true);
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            throw new IOException("Model JSON root is not an object");
        }
        reader.beginObject();
        while (reader.hasNext()) {
            final String name = reader.nextName();
            if ("ae2_uvl_marker".equals(name)) {
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
        return new Metadata(validUvlMarker && uvlMarker);
    }

    private static void logFailure(final Generation generation,
                                   final ResourceLocation location,
                                   final Throwable failure) {
        if (generation != null && generation.loggedFailures.add(location)) {
            NovaEngineeringCore.log.warn(
                "Failed to read model JSON metadata for {} in generation {}.",
                location, generation.id, failure);
        }
    }

    public synchronized void begin() {
        ++depth;
        activeGeneration = new Generation(++nextGeneration);
    }

    public boolean isActive() {
        return activeGeneration != null;
    }

    public synchronized void end() {
        if (depth > 0 && --depth == 0) {
            activeGeneration = null;
        }
    }

    public boolean hasUvlMarker(final ResourceLocation location) {
        return lookup(location).uvlMarker;
    }

    public boolean isQuerying(final ResourceLocation location) {
        final Query query = currentQuery.get();
        return query != null && query.location == location && query.generation != null;
    }

    /** Called only for a known archive after the real fallback manager selects its winning pack. */
    public InputStream openArchive(final ResourceLocation location, final Object source, final StreamOpener open)
        throws IOException {
        final Query query = currentQuery.get();
        if (query == null || query.location != location || query.generation == null) {
            return open.open();
        }
        final MetadataKey key = new MetadataKey(location, source);
        final Metadata cached = query.generation.metadata.get(key);
        if (cached != null) {
            // The manager or another wrapper can still request the original bytes. Closing an
            // unread stream on the normal metadata hit must not open the archive.
            return new SourcedResourceInputStream(new DeferredResourceInputStream(open), source, true);
        }
        return new SourcedResourceInputStream(open.open(), source, true);
    }

    private Metadata lookup(final ResourceLocation location) {
        final Query previous = currentQuery.get();
        try {
            Generation generation;
            Metadata result;
            do {
                generation = activeGeneration;
                currentQuery.set(new Query(location, generation));
                result = readResource(location, generation);
            } while (generation != activeGeneration);
            return result == null ? EMPTY : result;
        } finally {
            if (previous == null) {
                currentQuery.remove();
            } else {
                currentQuery.set(previous);
            }
        }
    }

    private Metadata readResource(final ResourceLocation location, final Generation generation) {
        try (IResource resource = resources.get().getResource(location)) {
            final InputStream stream = resource.getInputStream();
            if (generation == null || !(stream instanceof SourcedResourceStream sourced) || !sourced.isCacheableSource()) {
                return parse(stream, location, generation);
            }
            final MetadataKey key = new MetadataKey(location, sourced.sourceIdentity());
            return generation.metadata.computeIfAbsent(key, ignored -> parse(stream, location, generation));
        } catch (final FileNotFoundException ignored) {
            return EMPTY;
        } catch (final IOException | RuntimeException failure) {
            logFailure(generation, location, failure);
            return EMPTY;
        }
    }

    private record Query(ResourceLocation location, Generation generation) { }

    @FunctionalInterface
    public interface StreamOpener {
        InputStream open() throws IOException;
    }

    private static final class Generation {
        private final long id;
        private final ConcurrentHashMap<MetadataKey, Metadata> metadata = new ConcurrentHashMap<>();
        private final Set<ResourceLocation> loggedFailures = ConcurrentHashMap.newKeySet();

        private Generation(final long id) {
            this.id = id;
        }
    }

    private record MetadataKey(ResourceLocation location, Object sourceIdentity) {

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MetadataKey(ResourceLocation location1, Object identity))) {
                return false;
            }
            return this.sourceIdentity == identity
                && this.location.equals(location1);
        }

        @Override
        public int hashCode() {
            int result = this.location.hashCode();
            result = 31 * result + System.identityHashCode(this.sourceIdentity);
            return result;
        }
    }

    record Metadata(boolean uvlMarker) {
    }
}
