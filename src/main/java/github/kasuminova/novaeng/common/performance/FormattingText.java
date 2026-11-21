package github.kasuminova.novaeng.common.performance;

/** The ASCII, case-insensitive Minecraft formatting pattern, without allocating a regex matcher. */
public final class FormattingText {
    private FormattingText() {
    }

    public static String strip(final String text) {
        if (text == null) {
            return "";
        }
        StringBuilder result = null;
        int copied = 0;
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) == '§' && isCode(text.charAt(i + 1))) {
                if (result == null) {
                    result = new StringBuilder(text.length());
                }
                result.append(text, copied, i);
                copied = i + 2;
                i++;
            }
        }
        return result == null ? text : result.append(text, copied, text.length()).toString();
    }

    private static boolean isCode(final char c) {
        final char lower = (char) (c | 32);
        return c >= '0' && c <= '9' || lower >= 'a' && lower <= 'f' || lower >= 'k' && lower <= 'o' || lower == 'r';
    }
}
