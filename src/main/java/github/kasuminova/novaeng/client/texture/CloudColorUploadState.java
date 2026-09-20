package github.kasuminova.novaeng.client.texture;

/**
 * Tracks the last successfully uploaded one-pixel cloud color and its complete validity identity.
 */
public interface CloudColorUploadState {

    /**
     * Determines whether an upload is required for the supplied texture lifetime and world.
     *
     * @param texture texture object identity
     * @param generation texture deletion generation
     * @param argb exact packed color value
     * @param world client world identity
     * @return {@code true} unless every cached component is identical
     */
    boolean shouldUpload(Object texture, long generation, int argb, Object world);

    /**
     * Records a successfully completed upload.
     *
     * @param texture texture object identity
     * @param generation texture deletion generation after upload
     * @param argb exact packed color value
     * @param world client world identity
     */
    void record(Object texture, long generation, int argb, Object world);

    /** Clears all cached identities so the next color must upload. */
    void invalidate();
}
