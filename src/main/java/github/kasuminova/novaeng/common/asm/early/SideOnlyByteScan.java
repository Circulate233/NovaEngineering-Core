package github.kasuminova.novaeng.common.asm.early;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Exact byte equivalent of the SideTransformer's ISO-8859-1 String.contains predicate.
 */
public final class SideOnlyByteScan {
    private static final byte[] PATTERN = "Lnet/minecraftforge/fml/relauncher/SideOnly;".getBytes(StandardCharsets.ISO_8859_1);
    private static final int[] SKIP = new int[256];

    static {
        Arrays.fill(SKIP, PATTERN.length);
        for (int i = 0; i < PATTERN.length - 1; i++) SKIP[PATTERN[i] & 255] = PATTERN.length - 1 - i;
    }

    private SideOnlyByteScan() {
    }

    public static boolean containsSideOnly(final byte[] bytes) {
        // ISO-8859-1 maps every byte to one character, and this needle contains only ASCII.
        // Scan the whole input, including non-constant-pool bytes, just as the original does.
        final int length = PATTERN.length;
        final int limit = bytes.length - length;
        int start = 0;
        while (start <= limit) {
            final int end = start + length - 1;
            final int last = bytes[end] & 255;
            if (last == (PATTERN[length - 1] & 255)) {
                int offset = 1;
                while (offset < length && bytes[end - offset] == PATTERN[length - 1 - offset]) offset++;
                if (offset == length) return true;
            }
            // start + shift <= bytes.length, including arrays near the maximum int length.
            start += SKIP[last];
        }
        return false;
    }
}
