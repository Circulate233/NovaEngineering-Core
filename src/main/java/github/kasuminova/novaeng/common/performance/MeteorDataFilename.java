package github.kasuminova.novaeng.common.performance;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MeteorDataFilename {

    private static volatile DigitFormat lastFormat;

    private MeteorDataFilename() {
    }

    public static String encode(final int dimension, final int chunkX, final int chunkZ, final int scale,
                                final char separator, final char extensionSeparator, final String extension) {
        final Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        DigitFormat format = lastFormat;
        if (format == null || !format.locale().equals(locale)) {
            format = new DigitFormat(locale, DecimalFormatSymbols.getInstance(locale).getZeroDigit() == '0');
            lastFormat = format;
        }
        final int x = chunkX >> scale;
        final int z = chunkZ >> scale;
        if (format.ascii()) {
            return Integer.toString(dimension) + separator + x + separator + z + extensionSeparator + extension;
        }
        // Formatter localizes decimal digits. Keep that behavior when the runtime locale changes.
        return String.format(locale, "%d%c%d%c%d%c%s", dimension, separator, x, separator, z, extensionSeparator, extension);
    }

    private record DigitFormat(Locale locale, boolean ascii) {
    }
}
