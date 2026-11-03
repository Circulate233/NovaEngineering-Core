package github.kasuminova.novaeng.common.performance;

public final class ObjLineSyntax {
    private ObjLineSyntax() {
    }

    public static String collapseWhitespace(final String line) {
        int first = 0;
        while (first < line.length()) {
            final char c = line.charAt(first);
            if (whitespace(c) && (c != ' ' || first + 1 < line.length() && whitespace(line.charAt(first + 1)))) {
                break;
            }
            first++;
        }
        if (first == line.length()) {
            return line;
        }
        final StringBuilder result = new StringBuilder(line.length()).append(line, 0, first);
        for (int i = first; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (whitespace(c)) {
                result.append(' ');
                while (i + 1 < line.length() && whitespace(line.charAt(i + 1))) {
                    i++;
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static boolean numbers(final String line, final String prefix, final int minimum, final int maximum) {
        if (!line.startsWith(prefix)) {
            return false;
        }
        final int end = contentEnd(line);
        final boolean decimalRequired = prefix.equals("vt") && line.endsWith("\n");
        int offset = prefix.length();
        int count = 0;
        while (offset < end) {
            if (line.charAt(offset++) != ' ' || offset == end || ++count > maximum) {
                return false;
            }
            if (line.charAt(offset) == '-') {
                offset++;
            }
            final int integer = offset;
            while (offset < end && digit(line.charAt(offset))) {
                offset++;
            }
            if (offset == integer) {
                return false;
            }
            if (offset < end && line.charAt(offset) == '.') {
                final int decimal = ++offset;
                while (offset < end && digit(line.charAt(offset))) {
                    offset++;
                }
                if (decimal == offset) {
                    return false;
                }
            } else if (decimalRequired) {
                return false;
            }
        }
        return count >= minimum;
    }

    /**
     * 0 invalid, 1 vertex, 2 vertex/texture, 3 vertex/texture/normal, 4 vertex//normal.
     */
    public static int faceKind(final String line) {
        final int end = contentEnd(line);
        if (end < 2 || line.charAt(0) != 'f') {
            return 0;
        }
        int offset = 1;
        int count = 0;
        int kind = 0;
        while (offset < end) {
            if (line.charAt(offset++) != ' ' || ++count > 4) {
                return 0;
            }
            int begin = offset;
            while (offset < end && digit(line.charAt(offset))) {
                offset++;
            }
            if (offset == begin) {
                return 0;
            }
            int tokenKind = 1;
            if (offset < end && line.charAt(offset) == '/') {
                offset++;
                if (offset < end && line.charAt(offset) == '/') {
                    tokenKind = 4;
                    offset++;
                } else {
                    tokenKind = 2;
                    begin = offset;
                    while (offset < end && digit(line.charAt(offset))) {
                        offset++;
                    }
                    if (offset == begin) {
                        return 0;
                    }
                    if (offset < end && line.charAt(offset) == '/') {
                        tokenKind = 3;
                        offset++;
                    }
                }
                if (tokenKind == 3 || tokenKind == 4) {
                    begin = offset;
                    while (offset < end && digit(line.charAt(offset))) {
                        offset++;
                    }
                    if (offset == begin) {
                        return 0;
                    }
                }
            }
            if (kind != 0 && kind != tokenKind) {
                return 0;
            }
            kind = tokenKind;
        }
        return count >= 3 ? kind : 0;
    }

    public static boolean group(final String line) {
        final int end = contentEnd(line);
        if (end < 3 || line.charAt(0) != 'g' && line.charAt(0) != 'o' || line.charAt(1) != ' ') {
            return false;
        }
        for (int i = 2; i < end; i++) {
            final char c = line.charAt(i);
            if (!(digit(c) || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == '.')) {
                return false;
            }
        }
        return true;
    }

    private static int contentEnd(final String line) {
        int end = line.length();
        if (end != 0 && line.charAt(end - 1) == '\n') {
            end--;
        }
        while (end != 0 && line.charAt(end - 1) == ' ') {
            end--;
        }
        return end;
    }

    private static boolean digit(final char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean whitespace(final char c) {
        return c == ' ' || c >= '\t' && c <= '\r';
    }
}
