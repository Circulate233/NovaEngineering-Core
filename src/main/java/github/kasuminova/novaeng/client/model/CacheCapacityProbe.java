package github.kasuminova.novaeng.client.model;

import github.kasuminova.novaeng.NovaEngineeringCore;

/** Reports reload-scoped cache peaks so presets can be sized from the production pack. */
public final class CacheCapacityProbe {
    private CacheCapacityProbe() {
    }

    public static void report() {
        NovaEngineeringCore.log.info("Model cache capacity probe: {}, {}",
            PredicateCache.capacityProbe(), MultipartModelCache.capacityProbe());
    }
}
