package github.kasuminova.novaeng.client.performance;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ParsedStringMixinTest {
    @Test
    void topOnlySkipsTheOriginalParserForAnEqualCurrentConfigInput() throws Exception {
        final Method method = wrapper("theoneprobe.MixinConfigTextStyleCache", "nova$cachedTextStyle");
        final AtomicInteger calls = new AtomicInteger();
        final Operation<String> parser = arguments -> {
            calls.incrementAndGet();
            return "parsed:" + arguments[0];
        };
        assertEquals("parsed:cache-test-red", call(method, "cache-test-red", parser));
        assertEquals("parsed:cache-test-red", call(method, new String("cache-test-red"), parser));
        assertEquals(1, calls.get());
        assertEquals("parsed:cache-test-blue", call(method, "cache-test-blue", parser));
        assertEquals(2, calls.get());
    }

    @Test
    void invalidColorNullIsCachedButOriginalParserExceptionsAreNot() throws Exception {
        final Method method = wrapper("itemborders.MixinTextColorCache", "nova$cachedColor");
        final AtomicInteger calls = new AtomicInteger();
        final Operation<Integer> invalid = arguments -> { calls.incrementAndGet(); return null; };
        assertNull(call(method, "cache-test-invalid-color", invalid));
        assertNull(call(method, "cache-test-invalid-color", invalid));
        assertEquals(1, calls.get());
        final NullPointerException failure = new NullPointerException("original parser");
        final Operation<Integer> throwing = arguments -> { calls.incrementAndGet(); throw failure; };
        assertSame(failure, assertThrows(NullPointerException.class, () -> call(method, null, throwing)));
        assertSame(failure, assertThrows(NullPointerException.class, () -> call(method, null, throwing)));
        assertEquals(3, calls.get());
    }

    private static Method wrapper(final String type, final String name) throws Exception {
        final Class<?> mixin = Class.forName("github.kasuminova.novaeng.mixin." + type);
        final Method method = mixin.getDeclaredMethod(name, String.class, Operation.class);
        method.setAccessible(true);
        return method;
    }

    private static Object call(final Method method, final String input, final Operation<?> operation) throws Exception {
        try {
            return method.invoke(null, input, operation);
        } catch (final InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException failure) throw failure;
            throw exception;
        }
    }
}
