package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JourneyMapStateMemoTest {
    @Test
    void capturesCoordinatesBeforeMutablePositionAdvancesAndClearsWorldAtScopeExit() throws Exception {
        final Object world = new Object();
        final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(2, 70, 3);
        final JourneyMapStateMemo memo;
        try (final JourneyMapStateMemo scope = JourneyMapStateMemo.begin()) {
            memo = scope;
            memo.remember(world, position, null, null);
            assertTrue(memo.matches(world, new BlockPos(2, 70, 3), null));
            assertFalse(memo.matches(new Object(), position, null));
            position.setPos(3, 70, 3);
            assertFalse(memo.matches(world, position, null), "the same MutableBlockPos now denotes another block");
            assertTrue(memo.matches(world, new BlockPos(2, 70, 3), null));
        }
        assertNull(JourneyMapStateMemo.current());
        for (final Field field : JourneyMapStateMemo.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
            field.setAccessible(true);
            assertNull(field.get(memo), "idle memo must release " + field.getName());
        }
        try (final JourneyMapStateMemo reused = JourneyMapStateMemo.begin()) {
            assertSame(memo, reused);
            assertFalse(reused.matches(world, new BlockPos(2, 70, 3), null));
        }
    }

    @Test
    void nestedAndFailingQueriesRestoreTheirOwningScope() {
        final Object world = new Object();
        final BlockPos position = new BlockPos(1, 2, 3);
        try (final JourneyMapStateMemo outer = JourneyMapStateMemo.begin()) {
            outer.remember(world, position, null, null);
            try {
                try (final JourneyMapStateMemo inner = JourneyMapStateMemo.begin()) {
                    assertNotSame(outer, inner);
                    inner.remember(new Object(), position, null, null);
                    throw new IllegalStateException("nested extension failure");
                }
            } catch (final IllegalStateException expected) {
                assertSame(outer, JourneyMapStateMemo.current());
                assertTrue(outer.matches(world, position, null));
            }
        }
        assertNull(JourneyMapStateMemo.current());
    }
}
