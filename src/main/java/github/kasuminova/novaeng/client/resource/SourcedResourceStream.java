package github.kasuminova.novaeng.client.resource;

/**
 * Identifies the concrete resource pack which produced an opened resource stream.
 */
public interface SourcedResourceStream {

    /**
     * Returns the resource-pack object whose identity participates in metadata cache keys.
     *
     * @return source pack identity
     */
    Object sourceIdentity();

    /**
     * Reports whether this source is immutable for the active reload generation.
     *
     * @return {@code true} when parsed metadata may be shared
     */
    boolean isCacheableSource();
}
