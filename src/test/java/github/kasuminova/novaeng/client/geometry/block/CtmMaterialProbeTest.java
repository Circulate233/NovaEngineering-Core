package github.kasuminova.novaeng.client.geometry.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CtmMaterialProbeTest {
    @Test
    void vanillaBakeWithoutCtmScopeDoesNotUseTheProbe() {
        final Object model = new Object();
        final Object getter = new Object();

        CtmMaterialProbe.identifyGetter(model, getter);
        assertNull(CtmMaterialProbe.forGetter(getter));
        assertFalse(CtmMaterialProbe.isUnpublished(model, getter));
    }

    @Test
    void unpublishedGeometryIsScopedToTheMatchingModelAndGetter() {
        final Object ctm = new Object();
        final Object model = new Object();
        final Object getter = new Object();
        final IllegalStateException failure = new IllegalStateException();

        CtmMaterialProbe.run(ctm, () -> {
            CtmMaterialProbe.identifyGetter(ctm, getter);
            final CtmMaterialProbe probe = CtmMaterialProbe.forGetter(getter);
            assertNotNull(probe);
            assertFalse(CtmMaterialProbe.isUnpublished(model, getter));
            assertSame(failure, assertThrows(IllegalStateException.class, () -> probe.bake(model, model, () -> {
                assertTrue(CtmMaterialProbe.isUnpublished(model, getter));
                assertFalse(CtmMaterialProbe.isUnpublished(ctm, getter));
                assertFalse(CtmMaterialProbe.isUnpublished(model, new Object()));
                throw failure;
            })));
            assertFalse(CtmMaterialProbe.isUnpublished(model, getter));
            return null;
        });
        assertNull(CtmMaterialProbe.forGetter(getter));
    }
}
