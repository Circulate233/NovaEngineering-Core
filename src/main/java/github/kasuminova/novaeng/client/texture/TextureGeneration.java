package github.kasuminova.novaeng.client.texture;

/**
 * Exposes the lifetime generation of a Minecraft texture object.
 *
 * <p>The generation changes whenever the texture deletes its OpenGL allocation. Callers use it
 * to invalidate cached upload decisions even when OpenGL later reuses the same numeric texture
 * identifier.</p>
 */
public interface TextureGeneration {

    /**
     * Returns the number of deletion requests observed for this texture object.
     *
     * @return the current texture lifetime generation
     */
    long nova$getDeleteGeneration();
}
