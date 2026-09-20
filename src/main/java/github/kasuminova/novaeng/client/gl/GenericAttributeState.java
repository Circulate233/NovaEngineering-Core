package github.kasuminova.novaeng.client.gl;

/**
 * Tracks selected generic vertex constants and the normalized VAO binding for one GL thread.
 *
 * <p>Only COLOR and SECONDARY_UV constants participate in upload suppression. Draw-format scopes
 * make array enablement explicit; outside such a scope, uploads remain conservative.</p>
 */
public interface GenericAttributeState {

    /** Pushes the array enablement relevant to one shader pre-draw operation. */
    void pushDrawFormat(boolean colorArrayEnabled, boolean secondaryUvArrayEnabled);

    /** Marks array-backed constants undefined as required by OpenGL, then pops the draw scope. */
    void finishDrawFormat(boolean colorArrayEnabled, boolean secondaryUvArrayEnabled);

    /** Returns whether the COLOR constant must be uploaded for the active draw format. */
    boolean shouldUploadColor(int index, float x, float y, float z, float w);

    /** Returns whether the SECONDARY_UV constant must be uploaded for the active draw format. */
    boolean shouldUploadSecondaryUv(int index, float x, float y, float z, float w);

    /** Records a generic attribute setter after its backend call succeeds. */
    void recordAttribute(int index, float x, float y, float z, float w);

    /** Invalidates all tracked generic attribute values for the current context thread. */
    void invalidateAttributes();

    /** Returns whether a normalized VAO id still requires a backend bind. */
    boolean shouldBindVertexArray(int normalizedVertexArray);

    /** Records a normalized VAO id after its backend bind succeeds. */
    void recordBoundVertexArray(int normalizedVertexArray);

    /** Invalidates the current VAO shadow after raw binds, deletion, or context lifecycle changes. */
    void invalidateVertexArray();

    /** Invalidates both generic attribute and VAO state for a context lifecycle boundary. */
    void invalidateAll();
}
