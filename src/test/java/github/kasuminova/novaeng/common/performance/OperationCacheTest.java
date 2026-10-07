package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationCacheTest {
    @Test
    void nestedOperationsAreIsolatedAndReleaseReferences() {
        final OperationCache<Object, Object> cache = new OperationCache<>();
        final Object key = new Object();
        final Object outer = new Object();
        cache.enter();
        cache.put(key, outer);
        cache.enter();
        assertNull(cache.get(key));
        cache.put(key, new Object());
        cache.leave();
        assertSame(outer, cache.get(key));
        cache.leave();
        assertFalse(cache.active());
        assertNull(cache.get(key));
        cache.enter();
        assertNull(cache.get(key));
        cache.leave();
    }

    @Test
    void keyEqualityDoesNotAliasDistinctMutableStacks() {
        final OperationCache<Object, Object> cache = new OperationCache<>();
        cache.enter();
        final Object a = new String("same");
        cache.put(a, 1);
        assertNull(cache.get(new String("same")));
        cache.leave();
        assertThrows(IllegalStateException.class, cache::leave);
    }
}
