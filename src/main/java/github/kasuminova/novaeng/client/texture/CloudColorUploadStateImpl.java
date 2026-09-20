package github.kasuminova.novaeng.client.texture;

/** Identity-based implementation of {@link CloudColorUploadState}. */
public final class CloudColorUploadStateImpl implements CloudColorUploadState {

    private Object texture;
    private Object world;
    private long generation;
    private int argb;
    private boolean valid;

    @Override
    public boolean shouldUpload(final Object texture,
                                final long generation,
                                final int argb,
                                final Object world) {
        return !this.valid || this.texture != texture || this.world != world
            || this.generation != generation || this.argb != argb;
    }

    @Override
    public void record(final Object texture,
                       final long generation,
                       final int argb,
                       final Object world) {
        this.texture = texture;
        this.world = world;
        this.generation = generation;
        this.argb = argb;
        this.valid = true;
    }

    @Override
    public void invalidate() {
        this.texture = null;
        this.world = null;
        this.generation = 0L;
        this.argb = 0;
        this.valid = false;
    }
}
