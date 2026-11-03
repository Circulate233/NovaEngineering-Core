package github.kasuminova.novaeng.client.memory;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import neofontrender.core.font.cosmic.CosmicTextRenderer;

import java.lang.ref.WeakReference;

public final class DynamicFontCaches {
    private static final ObjectArrayList<WeakReference<CosmicTextRenderer>> RENDERERS = new ObjectArrayList<>();

    private DynamicFontCaches() {
    }

    public static void register(final CosmicTextRenderer renderer) {
        synchronized (RENDERERS) {
            RENDERERS.removeIf(reference -> reference.get() == null);
            RENDERERS.add(new WeakReference<>(renderer));
        }
    }

    public static void unregister(final CosmicTextRenderer renderer) {
        synchronized (RENDERERS) {
            RENDERERS.removeIf(reference -> reference.get() == null || reference.get() == renderer);
        }
    }

    public static void clearDynamicCaches() {
        final ObjectArrayList<CosmicTextRenderer> snapshot = new ObjectArrayList<>();
        synchronized (RENDERERS) {
            final var iterator = RENDERERS.iterator();
            while (iterator.hasNext()) {
                final CosmicTextRenderer renderer = iterator.next().get();
                if (renderer == null) iterator.remove();
                else snapshot.add(renderer);
            }
        }
        // Keep registry and renderer monitors separate; close() unregisters under the renderer monitor.
        for (final CosmicTextRenderer renderer : snapshot) renderer.clearCaches(false);
    }
}
