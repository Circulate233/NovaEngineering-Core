package github.kasuminova.novaeng.client.gl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericAttributeStateImplTest {

    private GenericAttributeState state;

    @BeforeEach
    void resetState() {
        this.state = GenericAttributeStateImpl.instance();
        this.state.invalidateAll();
    }

    @Test
    void arrayDrawMakesFollowingConstantUnknown() {
        this.state.pushDrawFormat(true, false);
        assertFalse(this.state.shouldUploadColor(1, 1.0F, 1.0F, 1.0F, 1.0F));
        this.state.finishDrawFormat(true, false);

        this.state.pushDrawFormat(false, false);
        assertTrue(this.state.shouldUploadColor(1, 1.0F, 1.0F, 1.0F, 1.0F));
        this.state.recordAttribute(1, 1.0F, 1.0F, 1.0F, 1.0F);
        assertFalse(this.state.shouldUploadColor(1, 1.0F, 1.0F, 1.0F, 1.0F));
        this.state.finishDrawFormat(false, false);
    }

    @Test
    void comparisonUsesExactRawFloatBits() {
        this.state.pushDrawFormat(false, false);
        this.state.shouldUploadColor(1, 0.0F, 0.0F, 0.0F, 1.0F);
        this.state.recordAttribute(1, 0.0F, 0.0F, 0.0F, 1.0F);
        assertTrue(this.state.shouldUploadColor(1, -0.0F, 0.0F, 0.0F, 1.0F));

        final float firstNaN = Float.intBitsToFloat(0x7fc00001);
        final float secondNaN = Float.intBitsToFloat(0x7fc00002);
        this.state.recordAttribute(1, firstNaN, 0.0F, 0.0F, 1.0F);
        assertFalse(this.state.shouldUploadColor(1, firstNaN, 0.0F, 0.0F, 1.0F));
        assertTrue(this.state.shouldUploadColor(1, secondNaN, 0.0F, 0.0F, 1.0F));
        this.state.finishDrawFormat(false, false);
    }

    @Test
    void vaoInvalidationForcesNextBind() {
        assertTrue(this.state.shouldBindVertexArray(7));
        this.state.recordBoundVertexArray(7);
        assertFalse(this.state.shouldBindVertexArray(7));
        this.state.invalidateVertexArray();
        assertTrue(this.state.shouldBindVertexArray(7));
    }
}
