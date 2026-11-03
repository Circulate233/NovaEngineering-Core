package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Content-addressed derived text only. A failed or corrupt cache never replaces the computation.
 */
public final class PersistentTextCache {
    private static final Logger LOGGER = LogManager.getLogger("NovaEngStartupCache");
    private static final int MAGIC = 0x4E545801;
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private final Path directory;
    private final String implementation;

    public PersistentTextCache(final Path directory, final String implementation) {
        this.directory = directory;
        this.implementation = implementation;
    }

    /**
     * Hash the entire defining artifacts, including nested helpers, parser tables and dependencies.
     */
    public static String implementationFingerprint(final Class<?>... anchors) {
        try {
            final MessageDigest digest = digest();
            update(digest, "nova-text-cache-v1/" + Runtime.version());
            final Set<Path> seen = new ObjectOpenHashSet<>();
            final byte[] buffer = new byte[65536];
            for (final Class<?> anchor : anchors) {
                final var codeSource = anchor.getProtectionDomain().getCodeSource();
                if (codeSource == null) {
                    return null;
                }
                var location = codeSource.getLocation();
                if ("jar".equals(location.getProtocol())) {
                    // Foundation records the class entry URL, not just the containing archive.
                    final var connection = location.openConnection();
                    connection.setUseCaches(false);
                    if (!(connection instanceof JarURLConnection jar)) {
                        return null;
                    }
                    location = jar.getJarFileURL();
                }
                if (!"file".equals(location.getProtocol())) {
                    return null;
                }
                final Path artifact = Path.of(location.toURI());
                if (!Files.isRegularFile(artifact)) {
                    return null; // Development class directories change independently of their directory timestamp.
                }
                if (seen.add(artifact)) {
                    update(digest, anchor.getName());
                    try (InputStream in = Files.newInputStream(artifact)) {
                        for (int count; (count = in.read(buffer)) != -1; ) {
                            digest.update(buffer, 0, count);
                        }
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception failure) {
            LOGGER.warn("Could not fingerprint text-transform implementation; persistent cache disabled", failure);
            return null;
        }
    }

    private static void update(final MessageDigest digest, final String value) {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        final int length = bytes.length;
        digest.update(new byte[]{(byte) (length >>> 24), (byte) (length >>> 16), (byte) (length >>> 8), (byte) length});
        digest.update(bytes);
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public String get(final String input, final String environment, final Supplier<String> compute) {
        if (implementation == null) {
            return compute.get();
        }
        final MessageDigest keyDigest = digest();
        update(keyDigest, implementation);
        update(keyDigest, environment);
        update(keyDigest, input);
        final byte[] key = keyDigest.digest();
        final Path entry = directory.resolve(HexFormat.of().formatHex(key) + ".bin");
        if (Files.isRegularFile(entry)) {
            try (DataInputStream in = new DataInputStream(Files.newInputStream(entry))) {
                if (in.readInt() != MAGIC || !MessageDigest.isEqual(key, in.readNBytes(32))) {
                    throw new IOException("Invalid cache header");
                }
                final int length = in.readInt();
                if (length < 0 || length > MAX_BYTES) {
                    throw new IOException("Invalid cache length");
                }
                final byte[] checksum = in.readNBytes(32);
                final byte[] value = in.readNBytes(length);
                if (value.length != length || in.read() != -1
                    || !MessageDigest.isEqual(checksum, digest().digest(value))) {
                    throw new IOException("Invalid cache checksum");
                }
                return new String(value, StandardCharsets.UTF_8);
            } catch (IOException | SecurityException failure) {
                LOGGER.warn("Ignoring unreadable derived-text cache {}", entry, failure);
            }
        }
        // Do not catch compute failures or persist the caller's error fallback.
        final String result = compute.get();
        final byte[] value = result.getBytes(StandardCharsets.UTF_8);
        if (value.length > MAX_BYTES) {
            return result;
        }
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, "writing-", ".tmp");
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(temporary))) {
                out.writeInt(MAGIC);
                out.write(key);
                out.writeInt(value.length);
                out.write(digest().digest(value));
                out.write(value);
            }
            try {
                Files.move(temporary, entry, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, entry, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | SecurityException failure) {
            LOGGER.warn("Could not save derived-text cache {}", entry, failure);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException | SecurityException failure) {
                    LOGGER.debug("Could not remove temporary cache {}", temporary, failure);
                }
            }
        }
        return result;
    }
}
