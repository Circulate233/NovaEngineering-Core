package github.kasuminova.novaeng.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileEntityVisibilityTest {
    @Test
    void measuresNearestPartOfTheBoxAndKeepsTheExactBoundary() {
        assertFalse(TileEntityVisibility.isBeyondDistance(64, 0, 0, 80, 16, 16, 0, 0, 0, 64));
        assertTrue(TileEntityVisibility.isBeyondDistance(64, 0, 0, 80, 16, 16, -0.01, 0, 0, 64));
        assertFalse(TileEntityVisibility.isBeyondDistance(64, 0, 0, 80, 16, 16, 72, 8, 8, 1));
    }

    @Test
    void largeStructuresStayVisibleEvenWhenTheirOriginIsVeryFarAway() {
        assertFalse(TileEntityVisibility.isBeyondDistance(500, 0, 0, 564, 4, 4, 0, 0, 0, 32));
    }

    @Test
    void nonFiniteAndInvertedBoundsAreNeverRejected() {
        assertFalse(TileEntityVisibility.hasFiniteBounds(0, 0, 0, Double.POSITIVE_INFINITY, 1, 1));
        assertFalse(TileEntityVisibility.hasFiniteBounds(Double.NaN, 0, 0, 1, 1, 1));
        assertFalse(TileEntityVisibility.hasFiniteBounds(2, 0, 0, 1, 1, 1));
        assertFalse(TileEntityVisibility.isBeyondDistance(Double.NaN, 0, 0, 1, 1, 1, 0, 0, 0, 32));
    }

    @Test
    void zeroDisablesOnlyTheDistancePolicy() {
        assertTrue(TileEntityVisibility.hasFiniteBounds(100, 0, 0, 101, 1, 1));
        assertFalse(TileEntityVisibility.isBeyondDistance(100, 0, 0, 101, 1, 1, 0, 0, 0, 0));
        assertTrue(TileEntityVisibility.isBeyondDistance(100, 0, 0, 101, 1, 1, 0, 0, 0, 32));
    }
}
