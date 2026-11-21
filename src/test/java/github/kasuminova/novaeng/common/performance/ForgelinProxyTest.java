package github.kasuminova.novaeng.common.performance;

import io.github.chaosunity.forgelin.KotlinAdapter;
import kotlin.Unit;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.ILanguageAdapter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class ForgelinProxyTest {
    public static Object proxy;
    private static Object inaccessibleProxy;

    @Test
    void kotlinObjectFastPathRetainsAssignmentAndAccessChecks() throws Exception {
        final var constructor = ForgelinObjectAdapter.class.getDeclaredConstructor(FMLModContainer.class, ILanguageAdapter.class);
        constructor.setAccessible(true);
        final KotlinAdapter baseline = new KotlinAdapter();
        final ILanguageAdapter optimized = constructor.newInstance(null, baseline);
        final var objectsField = ForgelinObjectAdapter.class.getDeclaredField("OBJECTS");
        objectsField.setAccessible(true);
        final Object getter = ((ClassValue<?>) objectsField.get(null)).get(Unit.class);
        final var handle = getter.getClass().getDeclaredMethod("handle");
        handle.setAccessible(true);
        assertNotNull(handle.invoke(getter), "Kotlin Unit exercises the proven object path");
        final Field target = ForgelinProxyTest.class.getField("proxy");
        final Object value = new Object();
        baseline.setProxy(target, Unit.class, value);
        assertSame(value, proxy);
        proxy = null;
        optimized.setProxy(target, Unit.class, value);
        assertSame(value, proxy);
        final Field inaccessible = ForgelinProxyTest.class.getDeclaredField("inaccessibleProxy");
        assertThrows(IllegalAccessException.class, () -> baseline.setProxy(inaccessible, Unit.class, value));
        assertThrows(IllegalAccessException.class, () -> optimized.setProxy(inaccessible, Unit.class, value));
        proxy = null;
    }

    @Test
    void customAdapterStillOwnsProxyAssignment() throws Exception {
        final boolean[] called = {false};
        final KotlinAdapter custom = new KotlinAdapter() {
            @Override public void setProxy(Field field, Class<?> target, Object value) {
                called[0] = true;
            }
        };
        final var constructor = ForgelinObjectAdapter.class.getDeclaredConstructor(FMLModContainer.class, ILanguageAdapter.class);
        constructor.setAccessible(true);
        final ILanguageAdapter optimized = constructor.newInstance(null, custom);
        optimized.setProxy(null, null, null);
        assertTrue(called[0]);
    }
}
