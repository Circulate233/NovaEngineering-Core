package github.kasuminova.novaeng.client.geometry.vertex;

/**
 * Explicit ownership boundary for a local Forge builder which has never escaped its producer.
 */
public interface CompactQuadBuilderAccess {
    OwnedUnpackedQuadRecord novaeng$buildRecord();
}
