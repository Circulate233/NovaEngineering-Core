package github.kasuminova.novaeng.common.performance;

import appeng.core.worlddata.MeteorDataNameEncoder;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MeteorDataFilenameTest {

    @Test
    void matchesActualEncoderAcrossLocalesSignedCoordinatesAndScaleBoundaries() {
        final Locale saved = Locale.getDefault(Locale.Category.FORMAT);
        final Random random = new Random(0xAE2F11EL);
        try {
            for (final Locale locale : new Locale[]{Locale.ROOT, Locale.US, Locale.CHINA, Locale.FRANCE,
                Locale.forLanguageTag("ar-EG"), Locale.forLanguageTag("fa-IR"), Locale.forLanguageTag("th-TH-u-nu-thai"), Locale.US}) {
                Locale.setDefault(Locale.Category.FORMAT, locale);
                for (final int scale : new int[]{0, 1, 4, 16, 31, 32, 63}) {
                    final MeteorDataNameEncoder original = new MeteorDataNameEncoder(scale);
                    for (int i = 0; i < 120; i++) {
                        final int dimension = i == 0 ? Integer.MIN_VALUE : random.nextInt();
                        final int x = i == 1 ? Integer.MAX_VALUE : random.nextInt();
                        final int z = i == 2 ? -1 : random.nextInt();
                        assertEquals(original.encode(dimension, x, z),
                            MeteorDataFilename.encode(dimension, x, z, scale, '_', '.', "dat"));
                    }
                }
            }
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, saved);
        }
    }

    @Test
    void preservesSeparatorsExtensionsAndLocaleChangesWithoutCachingNames() {
        final Locale saved = Locale.getDefault(Locale.Category.FORMAT);
        try {
            for (final Locale locale : new Locale[]{Locale.US, Locale.forLanguageTag("ar"), Locale.US}) {
                Locale.setDefault(Locale.Category.FORMAT, locale);
                for (final char separator : new char[]{'_', '-', '%', '\uFF0F'}) {
                    for (final String extension : new String[]{"dat", "meta.gz", "百分比%"}) {
                        final String expected = String.format("%d%c%d%c%d%c%s", -300, separator, -2, separator, 3, '.', extension);
                        assertEquals(expected, MeteorDataFilename.encode(-300, -17, 63, 4, separator, '.', extension));
                    }
                }
            }
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, saved);
        }
    }
}
