package github.kasuminova.novaeng.common.performance;

import com.sun.management.ThreadMXBean;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import stanhebben.zenscript.parser.CompiledDFA;
import stanhebben.zenscript.parser.NFA;
import stanhebben.zenscript.parser.TokenStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Standalone verification, with optional scripts directory; no client or Forge startup required.
 */
public final class ZenTokenerDfaVerification {
    private ZenTokenerDfaVerification() {
    }

    static void main(final String[] args) throws Exception {
        if (args.length > 0 && args[0].startsWith("--bench-")) {
            benchmark(args[0].equals("--bench-patched"));
            return;
        }
        final String[] regexps = (String[]) ZenTokenerDfaGenerator.field("REGEXPS");
        final int[] finals = (int[]) ZenTokenerDfaGenerator.field("FINALS");
        final CompiledDFA original = (CompiledDFA) ZenTokenerDfaGenerator.field("DFA");
        final byte[] resource;
        try (var stream = ZenTokenerDfa.class.getResourceAsStream(ZenTokenerDfa.RESOURCE)) {
            resource = stream.readAllBytes();
        }
        final CompiledDFA generated = ZenTokenerDfa.read(new ByteArrayInputStream(resource), regexps, finals);
        check(generated != null, "generated path was rejected");
        final int[] originalTable = ZenTokenerDfaGenerator.table(ZenTokenerDfaGenerator.canonical(original));
        final int[] generatedTable = ZenTokenerDfaGenerator.table(ZenTokenerDfaGenerator.canonical(generated));
        check(Arrays.equals(originalTable, generatedTable),
            "state/label/target/final graph differs at " + Arrays.mismatch(originalTable, generatedTable)
                + ", lengths " + originalTable.length + "/" + generatedTable.length);
        final String[] changedText = regexps.clone();
        changedText[0] += "x";
        check(ZenTokenerDfa.read(new ByteArrayInputStream(resource), changedText, finals) == null, "changed grammar accepted");
        final int[] changedPriority = finals.clone();
        changedPriority[0]++;
        check(ZenTokenerDfa.read(new ByteArrayInputStream(resource), regexps, changedPriority) == null, "changed token accepted");
        final String[] reordered = regexps.clone();
        final String first = reordered[0];
        reordered[0] = reordered[1];
        reordered[1] = first;
        check(ZenTokenerDfa.read(new ByteArrayInputStream(resource), reordered, finals) == null, "changed order accepted");
        check(ZenTokenerDfa.read(null, regexps, finals) == null, "missing artifact accepted");
        final byte[] changedParser = resource.clone();
        final var parserData = new DataInputStream(new ByteArrayInputStream(changedParser));
        parserData.readInt();
        final int grammarSize = parserData.readInt();
        for (int i = 0; i < grammarSize; i++) {
            parserData.readUTF();
            parserData.readInt();
        }
        parserData.readInt();
        parserData.readUTF();
        changedParser[changedParser.length - parserData.available()] ^= 1;
        final byte[] checksum = MessageDigest.getInstance("SHA-256")
                                                           .digest(Arrays.copyOf(changedParser, changedParser.length - 32));
        System.arraycopy(checksum, 0, changedParser, changedParser.length - 32, 32);
        check(ZenTokenerDfa.read(new ByteArrayInputStream(changedParser), regexps, finals) == null, "changed parser fingerprint accepted");
        resource[resource.length / 2] ^= 1;
        check(ZenTokenerDfa.read(new ByteArrayInputStream(resource), regexps, finals) == null, "corrupt artifact accepted");
        check(Arrays.equals(ZenTokenerDfaGenerator.table(ZenTokenerDfaGenerator.canonical(ZenTokenerDfa.compile(changedText, finals))),
                ZenTokenerDfaGenerator.table(ZenTokenerDfaGenerator.canonical(new NFA(changedText, finals).toDFA().optimize().compile()))),
            "fallback changed compiler result");
        for (final String text : List.of("", " ", "\n\r\n", "//comment", "/* a * b */", "/* unterminated", "\"unterminated",
            "var a = 12.3e-2; a += 0x1f; a <<= 1;", "a..b.a a==b != c <= d >= e && f || g", "\"a\\n\\u1234\" 'b'",
            "中文 Ā Ȁ \uFFFF 😀", "\u0000\u0001\u007f\u0080\u00ff\u0100\u0101"))
            compare(original, generated, text);
        final Random random = new Random(20261126L);
        final String alphabet = "abcxyz_019eE-+*/=<>!|&^~:;.,()[]{}$? \t\r\n\"'\\中文\u0000\u00ff\u0100\uffff";
        for (int i = 0; i < 2000; i++) {
            final StringBuilder text = new StringBuilder();
            for (int j = random.nextInt(180); j > 0; j--)
                text.append(alphabet.charAt(random.nextInt(alphabet.length())));
            compare(original, generated, text.toString());
        }
        int scripts = 0;
        if (args.length > 0) {
            try (var paths = Files.walk(Path.of(args[0]))) {
                for (final Path path : paths.filter(path -> path.toString().endsWith(".zs")).toList()) {
                    compare(original, generated, Files.readString(path));
                    scripts++;
                }
            }
        }
        verifyTransformation();
        System.out.println("PASS graph equivalence, grammar/order/token fallback, corruption, 2000 random inputs, " + scripts + " scripts, transformed class");
    }

    private static void check(final boolean condition, final String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void compare(final CompiledDFA original, final CompiledDFA generated, final String text) throws Exception {
        check(tokens(original, text).equals(tokens(generated, text)), "token/error/position difference: " + text);
    }

    private static String tokens(final CompiledDFA dfa, final String text) throws Exception {
        final StringBuilder result = new StringBuilder();
        try {
            final TokenStream stream = new TokenStream(text, dfa);
            while (stream.hasNext()) {
                final var token = stream.next();
                result.append(token.getType()).append(':').append(token.getValue()).append('@').append(token.getPosition()).append('\n');
            }
            result.append("EOF").append('@').append(stream.getLine()).append(':').append(stream.getLineOffset());
        } catch (RuntimeException error) {
            result.append(error.getClass().getName()).append(':').append(error.getMessage());
        }
        return result.toString();
    }

    private static byte[] transformed(final boolean patch) throws Exception {
        try (var stream = CompiledDFA.class.getResourceAsStream("/stanhebben/zenscript/ZenTokener.class")) {
            final byte[] bytes = stream.readAllBytes();
            final ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            if (!patch) return bytes;
            check(ZenTokenerDfaTransform.apply(node), "expected pattern missing");
            check(!ZenTokenerDfaTransform.apply(node), "patch was applied twice");
            final ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            return writer.toByteArray();
        }
    }

    private static Class<?> initialize(final byte[] bytes) throws Exception {
        final ClassLoader loader = new ClassLoader(ZenTokenerDfaVerification.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                if (!name.equals("stanhebben.zenscript.ZenTokener")) return super.loadClass(name, resolve);
                Class<?> type = findLoadedClass(name);
                if (type == null) type = defineClass(name, bytes, 0, bytes.length);
                if (resolve) resolveClass(type);
                return type;
            }
        };
        return Class.forName("stanhebben.zenscript.ZenTokener", true, loader);
    }

    private static void verifyTransformation() throws Exception {
        final var type = initialize(transformed(true));
        final var field = type.getDeclaredField("DFA");
        field.setAccessible(true);
        check(Arrays.equals(ZenTokenerDfaGenerator.table((CompiledDFA) field.get(null)),
            ZenTokenerDfaGenerator.table(ZenTokenerDfaGenerator.canonical((CompiledDFA) ZenTokenerDfaGenerator.field("DFA")))), "patched initializer differs");
    }

    private static void benchmark(final boolean patch) throws Exception {
        final byte[] bytes = transformed(patch);
        // The client has initialized logging long before ZenTokener. Exclude logging bootstrap in both arms.
        LogManager.getLogger("NovaEng/ZenTokenerDfa");
        final var bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        final long allocated = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
        final long start = System.nanoTime();
        initialize(bytes);
        System.out.printf("%s %.3f ms %.3f MiB%n", patch ? "patched" : "original", (System.nanoTime() - start) / 1e6,
            (bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - allocated) / 1048576.0);
    }
}
