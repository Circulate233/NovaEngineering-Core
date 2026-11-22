package github.kasuminova.novaeng.client.geometry;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeometryWithoutActiniumTest {
    private static final String QUAD_VIEW = "dhj.embeddedt.embeddium.impl.model.quad.BakedQuadView";

    @Test
    void vanillaBakeAndNullifierCacheRunWithoutActiniumOnTheClasspath() throws Throwable {
        final var urls = new LinkedHashSet<URL>();
        for (ClassLoader loader = getClass().getClassLoader(); loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urlLoader) urls.addAll(List.of(urlLoader.getURLs()));
        }
        for (final String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
            urls.add(Path.of(path).toUri().toURL());
        }
        try (final var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader()) {
            @Override
            protected Class<?> findClass(final String name) throws ClassNotFoundException {
                if (withoutActinium(name.replace('.', '/'))) throw new ClassNotFoundException(name);
                return super.findClass(name);
            }

            @Override
            public URL findResource(final String name) {
                return withoutActinium(name) ? null : super.findResource(name);
            }
        }) {
            assertThrows(ClassNotFoundException.class, () -> loader.loadClass(QUAD_VIEW));
            assertNull(loader.getResource(QUAD_VIEW.replace('.', '/') + ".class"));
            run(loader, "geometry.block.CompactBlockModelTest", "propertiesOrderAndStableWritablePublicationArePreserved");
            run(loader, "enderutilities.NullifierBakedModelTest", "emptySelectedAndLockedShellsComposeWithoutMutatingTheirInputs");
        }
    }

    private static boolean withoutActinium(final String name) {
        return name.startsWith("com/dhj/actinium/") || name.startsWith("dhj/embeddedt/");
    }

    private static void run(final ClassLoader loader, final String fixture, final String method) throws Throwable {
        final Class<?> type = loader.loadClass("github.kasuminova.novaeng.client." + fixture);
        final var constructor = type.getDeclaredConstructor();
        final var check = type.getDeclaredMethod(method);
        constructor.setAccessible(true);
        check.setAccessible(true);
        try {
            check.invoke(constructor.newInstance());
        } catch (final InvocationTargetException error) {
            throw error.getCause();
        }
    }
}
