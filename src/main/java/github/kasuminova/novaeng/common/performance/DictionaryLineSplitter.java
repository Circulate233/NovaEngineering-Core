package github.kasuminova.novaeng.common.performance;

/**
 * Equivalent to String.split("[\\t ]+") for the dictionary's fixed delimiter, without a regex matcher.
 */
public final class DictionaryLineSplitter {

    private static final String[] EMPTY = new String[0];

    private DictionaryLineSplitter() {
    }

    public static String[] split(final String line) {
        final int length = line.length();
        if (length == 0) {
            return new String[]{line};
        }
        int end = length;
        while (end > 0 && isDelimiter(line.charAt(end - 1))) {
            end--;
        }
        if (end == 0) {
            return EMPTY;
        }
        int count = 1;
        for (int i = 0; i < end; i++) {
            if (isDelimiter(line.charAt(i))) {
                count++;
                while (i + 1 < end && isDelimiter(line.charAt(i + 1))) {
                    i++;
                }
            }
        }
        final String[] fields = new String[count];
        int field = 0;
        int start = 0;
        for (int i = 0; i < end; i++) {
            if (isDelimiter(line.charAt(i))) {
                fields[field++] = line.substring(start, i);
                while (i + 1 < end && isDelimiter(line.charAt(i + 1))) {
                    i++;
                }
                start = i + 1;
            }
        }
        fields[field] = line.substring(start, end);
        return fields;
    }

    private static boolean isDelimiter(final char character) {
        return character == ' ' || character == '\t';
    }
}
