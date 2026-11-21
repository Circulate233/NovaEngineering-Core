package github.kasuminova.novaeng.client.font;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextRouteHashTest {
    @Test
    void unboxedHashMatchesTheOriginalAcrossAllFieldsAndOverflow() throws Exception {
        final Class<?> key = Class.forName("neofontrender.api.text.route.TextRenderRouteApi$CacheKey");
        final var constructor = key.getDeclaredConstructor(String.class, int.class, boolean.class, int.class,
            long.class, long.class, int.class, int.class, long.class);
        constructor.setAccessible(true);
        final var hashCode = key.getDeclaredMethod("hashCode");
        hashCode.setAccessible(true);
        final Random random = new Random(0x20261119L);
        final String[] sources = {null, "", "Nova", "中文字体", "\uD83D\uDE00", "Aa", "BB"};
        for (int i = 0; i < 5000; i++) {
            final String source = sources[i % sources.length];
            final int argb = random.nextInt(), fontSize = random.nextInt();
            final long revision = random.nextLong(), structured = random.nextLong();
            final int raster = random.nextInt(), engine = random.nextInt();
            final long backend = random.nextLong();
            final boolean shadow = (i & 1) == 0;
            final int expected = Objects.hash(source, argb, shadow, fontSize, revision, structured, raster, engine, backend);
            assertEquals(expected, hashCode.invoke(constructor.newInstance(source, argb, shadow, fontSize,
                revision, structured, raster, engine, backend)), "Keep the dependency's original hash contract");
            assertEquals(expected, TextRouteHash.hash(source, argb, shadow, fontSize, revision, structured, raster, engine, backend));
        }
    }
}
