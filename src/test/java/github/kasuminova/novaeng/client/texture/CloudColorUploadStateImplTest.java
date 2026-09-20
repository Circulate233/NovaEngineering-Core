package github.kasuminova.novaeng.client.texture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudColorUploadStateImplTest {

    @Test
    void everyIdentityComponentAndInvalidationControlsUpload() {
        final CloudColorUploadState state = new CloudColorUploadStateImpl();
        final Object texture = new Object();
        final Object world = new Object();

        assertTrue(state.shouldUpload(texture, 0L, 0xff102030, world));
        state.record(texture, 0L, 0xff102030, world);
        assertFalse(state.shouldUpload(texture, 0L, 0xff102030, world));
        assertTrue(state.shouldUpload(new Object(), 0L, 0xff102030, world));
        assertTrue(state.shouldUpload(texture, 1L, 0xff102030, world));
        assertTrue(state.shouldUpload(texture, 0L, 0xff102031, world));
        assertTrue(state.shouldUpload(texture, 0L, 0xff102030, new Object()));

        state.invalidate();
        assertTrue(state.shouldUpload(texture, 0L, 0xff102030, world));
    }
}
