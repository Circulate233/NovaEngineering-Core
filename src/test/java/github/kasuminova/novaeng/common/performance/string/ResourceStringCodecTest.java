package github.kasuminova.novaeng.common.performance.string;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ResourceStringCodecTest {
    private static void check(final String value) {
        assertArrayEquals(originalSplit(value), ResourceStringCodec.split(value), value);
        final String[] model = {null, value, null};
        final int hash = value.indexOf('#');
        String location = value;
        if (hash >= 0) {
            model[2] = value.substring(hash + 1);
            if (hash > 1) location = value.substring(0, hash);
        }
        System.arraycopy(originalSplit(location), 0, model, 0, 2);
        assertArrayEquals(model, ResourceStringCodec.model(value), value + Arrays.toString(model));
    }

    private static String[] originalSplit(final String value) {
        final String[] parts = {"minecraft", value};
        final int colon = value.indexOf(':');
        if (colon >= 0) {
            parts[1] = value.substring(colon + 1);
            if (colon > 1) parts[0] = value.substring(0, colon);
        }
        return parts;
    }

    @Test
    void parserKeepsLegacyBoundaryAndCaseBehavior() {
        for (final String input : List.of("", ":", "a:b", "ab:c", "#", "a#b", "ab#c", "a:b#c", "::#", "AB:CD#Inventory", "İ:Σ#N", "a##b"))
            check(input);
        final Random random = new Random(27271);
        for (int i = 0; i < 10000; i++) {
            final StringBuilder input = new StringBuilder();
            for (int n = random.nextInt(32); n > 0; n--) input.append("ab:##AZ/İΣ".charAt(random.nextInt(10)));
            check(input.toString());
        }
    }

    @Test
    void repeatedPartsShareStringsWithoutSharingMutableArrays() {
        final String[] first = ResourceStringCodec.model("tconstruct:tool#inventory");
        final String[] second = ResourceStringCodec.model("tconstruct:tool#inventory");
        assertNotSame(first, second);
        for (int i = 0; i < first.length; i++) assertSame(first[i], second[i]);
        second[1] = "changed";
        assertEquals("tool", first[1]);
    }
}
