package github.kasuminova.novaeng.common.performance;

import stanhebben.zenscript.ZenTokener;
import stanhebben.zenscript.parser.CompiledDFA;
import stanhebben.zenscript.parser.HashMapII;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Offline tool: java ...ZenTokenerDfaGenerator <upstream CraftTweaker.jar> <output.dfa>.
 */
public final class ZenTokenerDfaGenerator {
    private ZenTokenerDfaGenerator() {
    }

    static void main(final String[] args) throws Exception {
        final var bytes = new ByteArrayOutputStream();
        final var output = new DataOutputStream(bytes);
        final String[] regexps = (String[]) field("REGEXPS");
        final int[] finals = (int[]) field("FINALS");
        output.writeInt(ZenTokenerDfa.MAGIC);
        output.writeInt(regexps.length);
        for (int i = 0; i < regexps.length; i++) {
            output.writeUTF(regexps[i]);
            output.writeInt(finals[i]);
        }
        try (JarFile jar = new JarFile(args[0])) {
            // Pin the parser implementation as well as grammar; an upstream parser change must fall back.
            final var entries = jar.stream().filter(entry -> entry.getName().startsWith("stanhebben/zenscript/parser/")
                && entry.getName().endsWith(".class")).sorted(Comparator.comparing(JarEntry::getName)).toList();
            output.writeInt(entries.size());
            for (final var entry : entries) {
                output.writeUTF("/" + entry.getName());
                try (final var code = jar.getInputStream(entry)) {
                    output.write(MessageDigest.getInstance("SHA-256").digest(code.readAllBytes()));
                }
            }
        }
        final int[] table = table(canonical((CompiledDFA) field("DFA")));
        output.writeInt(table.length);
        for (final int value : table) output.writeInt(value);
        output.flush();
        final byte[] payload = bytes.toByteArray();
        output.write(MessageDigest.getInstance("SHA-256").digest(payload));
        final Path path = Path.of(args[1]);
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.write(path, bytes.toByteArray());
        System.out.println("Generated " + bytes.size() + " bytes, " + table.length + " table ints");
    }

    static Object field(final String name) throws Exception {
        final Field field = ZenTokener.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    static int[] table(final CompiledDFA dfa) {
        // Upstream toArray() groups ranges using array indexes as character labels. It is not a lossless
        // serializer for this grammar. Emit the constructor's uncompressed edge representation instead.
        final var values = new ArrayList<Integer>();
        values.add(dfa.finals.length);
        for (int i = 0; i < dfa.finals.length; i++) {
            values.add(dfa.finals[i]);
            values.add(0); // no compressed ranges
            final int[] labels = dfa.transitions[i].keysArray();
            Arrays.sort(labels);
            values.add(labels.length);
            for (final int label : labels) {
                values.add(label);
                values.add(dfa.transitions[i].get(label));
            }
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Original state identity/order is unspecified. Canonical BFS and sorted labels make regeneration reproducible.
     */
    static CompiledDFA canonical(final CompiledDFA original) {
        final var order = new ArrayList<Integer>();
        final var ids = new HashMap<Integer, Integer>();
        order.add(0);
        ids.put(0, 0);
        for (int i = 0; i < order.size(); i++) {
            final HashMapII edges = original.transitions[order.get(i)];
            final int[] labels = edges.keysArray();
            Arrays.sort(labels);
            for (final int label : labels) {
                final int target = edges.get(label);
                if (!ids.containsKey(target)) {
                    ids.put(target, order.size());
                    order.add(target);
                }
            }
        }
        final HashMapII[] transitions = new HashMapII[order.size()];
        final int[] finals = new int[order.size()];
        for (int i = 0; i < order.size(); i++) {
            transitions[i] = new HashMapII();
            finals[i] = original.finals[order.get(i)];
            final HashMapII edges = original.transitions[order.get(i)];
            final int[] labels = edges.keysArray();
            Arrays.sort(labels);
            for (final int label : labels) transitions[i].put(label, ids.get(edges.get(label)));
        }
        return new CompiledDFA(transitions, finals);
    }
}
