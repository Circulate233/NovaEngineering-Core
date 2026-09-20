package github.kasuminova.novaeng.client.resource;

import net.minecraft.util.ResourceLocation;

/**
 * Shares model JSON parsing between loaders during one resource reload generation.
 *
 * <p>Every query still resolves the winning resource source through the live resource manager.
 * Only parsed metadata from an explicitly static winning pack is reused, so dynamic overlays keep
 * their normal priority and update semantics. This index does not eliminate resource lookup.</p>
 */
public interface ModelJsonMetadataIndex {

    /**
     * Starts a fresh metadata generation and rejects nested reload ownership.
     */
    void begin();

    /**
     * Reports whether loader mixins may answer from this generation.
     *
     * @return {@code true} while a model reload is active
     */
    boolean isActive();

    /**
     * Tests the LibNine {@code 9s} discriminator in the selected model JSON.
     *
     * @param location exact resource location consumed by LibNine
     * @param type requested discriminator value
     * @return whether the parsed string equals {@code type}
     */
    boolean isLibNineType(ResourceLocation location, String type);

    /**
     * Tests the AE2 {@code ae2_uvl_marker} value in a normalized model JSON resource.
     *
     * @param location normalized {@code models/...json} resource location
     * @return parsed marker value, or {@code false} when absent or invalid
     */
    boolean hasUvlMarker(ResourceLocation location);

    /**
     * Ends the generation and releases cached metadata and pack identities.
     */
    void end();
}
