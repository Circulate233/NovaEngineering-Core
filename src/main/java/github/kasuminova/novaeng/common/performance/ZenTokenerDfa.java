package github.kasuminova.novaeng.common.performance;

import org.apache.logging.log4j.LogManager;
import stanhebben.zenscript.parser.CompiledDFA;
import stanhebben.zenscript.parser.NFA;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Optional, generated lexer table. No cache survives ZenTokener initialization.
 */
public final class ZenTokenerDfa {
    static final String RESOURCE = "/assets/novaeng/zenscript/zentokener.dfa";
    static final int MAGIC = 0x5A444631;
    private static final int MAX_RESOURCE_BYTES = 1 << 20;

    private ZenTokenerDfa() {
    }

    public static CompiledDFA compile(final String[] regexps, final int[] finals) {
        try (InputStream input = ZenTokenerDfa.class.getResourceAsStream(RESOURCE)) {
            final CompiledDFA generated = read(input, regexps, finals);
            if (generated != null) {
                LogManager.getLogger("NovaEng/ZenTokenerDfa")
                          .debug("Loaded generated ZenTokener DFA (exact grammar and parser fingerprints matched)");
                return generated;
            }
        } catch (IOException | NoSuchAlgorithmException | RuntimeException ignored) {
            // Missing, stale or damaged resources must retain the upstream compiler semantics.
        }
        return new NFA(regexps, finals).toDFA().optimize().compile();
    }

    static CompiledDFA read(final InputStream input, final String[] regexps, final int[] finals)
        throws IOException, NoSuchAlgorithmException {
        if (input == null) return null;
        final byte[] bytes = input.readNBytes(MAX_RESOURCE_BYTES + 1);
        if (bytes.length < 36 || bytes.length > MAX_RESOURCE_BYTES) return null;
        final int length = bytes.length - 32;
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes, 0, length);
        if (!MessageDigest.isEqual(digest.digest(), Arrays.copyOfRange(bytes, length, bytes.length))) return null;
        final DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes, 0, length));
        if (data.readInt() != MAGIC) return null;
        final int grammarSize = data.readInt();
        if (grammarSize != regexps.length || grammarSize != finals.length) return null;
        for (int i = 0; i < grammarSize; i++) {
            // Exact text, order and token priorities; no hash collision can accept another grammar.
            if (!data.readUTF().equals(regexps[i]) || data.readInt() != finals[i]) return null;
        }
        final int classes = data.readInt();
        if (classes < 1 || classes > 64) return null;
        for (int i = 0; i < classes; i++) {
            final String resource = data.readUTF();
            final byte[] expected = data.readNBytes(32);
            try (InputStream code = CompiledDFA.class.getResourceAsStream(resource)) {
                if (code == null) return null;
                digest.reset();
                final byte[] buffer = new byte[8192];
                int count;
                while ((count = code.read(buffer)) != -1) digest.update(buffer, 0, count);
                if (!MessageDigest.isEqual(expected, digest.digest())) return null;
            }
        }
        final int size = data.readInt();
        if (size < 1 || size > data.available() / 4) return null;
        final int[] table = new int[size];
        for (int i = 0; i < size; i++) table[i] = data.readInt();
        if (data.available() != 0 || !validTable(table)) return null;
        return new CompiledDFA(table);
    }

    private static boolean validTable(final int[] table) {
        final int states = table[0];
        if (states < 1 || states > (table.length - 1) / 3) return false;
        int offset = 1;
        for (int i = 0; i < states; i++) {
            if (offset > table.length - 3) return false;
            offset++; // final token
            if (table[offset++] != 0) return false; // generator emits no compressed ranges
            final int edges = table[offset++];
            if (edges < 0 || edges > (table.length - offset) / 2) return false;
            for (int j = 0; j < edges; j++) {
                offset++; // character label
                final int target = table[offset++];
                if (target < 0 || target >= states) return false;
            }
        }
        return offset == table.length;
    }
}
