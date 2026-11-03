package github.kasuminova.novaeng.client.gl;

/**
 * A binding captured only after a bind and for one upload body, never across progress/splash callbacks.
 */
public final class TextureUploadBindingScope {

    private static final ThreadLocal<int[]> BINDING = ThreadLocal.withInitial(() -> new int[]{-1});

    private TextureUploadBindingScope() {
    }

    public static int begin(final int texture) {
        final int[] binding = BINDING.get();
        final int previous = binding[0];
        binding[0] = texture;
        return previous;
    }

    public static int current() {
        return BINDING.get()[0];
    }

    public static void end(final int previous) {
        BINDING.get()[0] = previous;
    }
}
