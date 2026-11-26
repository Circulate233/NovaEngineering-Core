package github.kasuminova.novaeng.mixin.bogosorter;

import github.kasuminova.novaeng.common.performance.BogoOreNameParser;
import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OreNameParserTest {
    private static String original(final String name, final Set<String> configured) {
        if (configured.contains(name)) return null;
        final ArrayList<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        for (final char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token = new StringBuilder().append(c);
                } else tokens.add(Character.toString(c));
            } else token.append(c);
        }
        if (token.length() > 0) tokens.add(token.toString());
        final StringBuilder prefix = new StringBuilder();
        String match = null;
        for (final String part : tokens) {
            prefix.append(part);
            if (configured.contains(prefix.toString())) match = prefix.toString();
        }
        return match;
    }

    @Test
    void overwritesActualRegistrationMethod() throws Exception {
        final byte[][] bytes = IsolatedMixinTransformer.transformInPackage("github.kasuminova.novaeng.mixin.bogosorter",
            new String[]{"com.cleanroommc.bogosorter.common.OreDictHelper"}, "MixinOreDictHelper");
        final ClassNode target = new ClassNode();
        new ClassReader(bytes[0]).accept(target, 0);
        int calls = 0;
        for (final var method : target.methods) {
            if (!method.name.equals("onItemRegistration")) continue;
            for (final var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.name.equals("findPrefix")) calls++;
            }
        }
        assertEquals(1, calls);
    }

    @Test
    void matchesOriginalTokenizationIncludingLeadingUppercaseAndUnicode() {
        final Random random = new Random(7951623);
        final String alphabet = "abAZ_19\u00c9\u01c5\u0394\ud801\udc00";
        for (int trial = 0; trial < 10000; trial++) {
            final StringBuilder name = new StringBuilder();
            for (int j = random.nextInt(30); j > 0; j--)
                name.append(alphabet.charAt(random.nextInt(alphabet.length())));
            final Set<String> prefixes = new HashSet<>();
            for (int i = 0; i <= name.length(); i++) {
                if (random.nextBoolean()) prefixes.add(name.substring(0, i));
            }
            final String input = name.toString();
            assertEquals(original(input, prefixes), BogoOreNameParser.findPrefix(input, prefixes::contains), input);
        }
    }

    @Test
    void observesPrefixConfigurationChangesWithoutRetainingNames() {
        final Set<String> prefixes = new HashSet<>(Set.of("ingot"));
        assertEquals("ingot", BogoOreNameParser.findPrefix("ingotHotIron", prefixes::contains));
        prefixes.add("ingotHot");
        assertEquals("ingotHot", BogoOreNameParser.findPrefix("ingotHotIron", prefixes::contains));
        prefixes.clear();
        assertNull(BogoOreNameParser.findPrefix("ingotHotIron", prefixes::contains));
    }
}
