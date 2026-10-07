package github.kasuminova.novaeng.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeakIdentityMemoTest {
    @Test
    void equalValuesDoNotConfuseWorldIdentity() {
        final WeakIdentityMemo<Object> memo = new WeakIdentityMemo<>();
        final Object first = new String("dimension-0");
        final Object second = new String("dimension-0");
        assertTrue(memo.select(first));
        assertFalse(memo.select(first));
        assertTrue(memo.select(second));
        memo.clearIf(first);
        assertFalse(memo.select(second));
        memo.clearIf(second);
        assertTrue(memo.select(second));
    }

    @Test
    void disconnectAndReentryResetTheMemo() {
        final WeakIdentityMemo<Object> memo = new WeakIdentityMemo<>();
        final Object world = new Object();
        assertTrue(memo.select(world));
        memo.clear();
        assertTrue(memo.select(world));
        assertFalse(memo.select(null));
        assertTrue(memo.select(world));
    }
}
