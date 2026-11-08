package github.kasuminova.novaeng.common.asm.early;

import org.objectweb.asm.ClassReader;

/** Helpers for Forge's early transformers; deliberately independent of Forge configuration and game classes. */
public final class EventClassScan {
    private static final String SUBSCRIBER = "Lnet/minecraftforge/fml/common/eventhandler/SubscribeEvent;";

    private EventClassScan() {
    }

    public static boolean maySubscribe(final byte[] bytes) {
        if (bytes == null) return false;
        if (bytes.length < 10 || readInt(bytes, 0) != 0xCAFEBABE || readUnsignedShort(bytes, 6) > 69) return true;
        try {
            final int count = readUnsignedShort(bytes, 8);
            if (count == 0) return true;
            int offset = 10;
            for (int index = 1; index < count; index++) {
                switch (bytes[offset++] & 0xFF) {
                    case 1 -> {
                        final int length = readUnsignedShort(bytes, offset);
                        offset += 2;
                        if (offset > bytes.length - length) return true;
                        if (length == SUBSCRIBER.length() && matches(bytes, offset)) return true;
                        offset += length;
                    }
                    case 3, 4, 9, 10, 11, 12, 17, 18 -> offset += 4;
                    case 5, 6 -> { offset += 8; index++; }
                    case 7, 8, 16, 19, 20 -> offset += 2;
                    case 15 -> offset += 3;
                    default -> { return true; } // Let Forge/ASM retain diagnostics for unknown formats.
                }
                if (offset > bytes.length) return true;
            }
            // An annotation descriptor must occur in the constant pool. False positives use Forge's full parser.
            return false;
        } catch (IndexOutOfBoundsException malformed) {
            return true;
        }
    }

    public static String eventSuperclass(final ClassReader reader) {
        final String parent = reader.getSuperName();
        // java.lang.Object cannot be an Event. All other parents retain Forge's actual class-loading check.
        return "java/lang/Object".equals(parent) ? null : parent;
    }

    private static boolean matches(final byte[] bytes, final int offset) {
        for (int index = 0; index < SUBSCRIBER.length(); index++) {
            if (bytes[offset + index] != SUBSCRIBER.charAt(index)) return false;
        }
        return true;
    }

    private static int readUnsignedShort(final byte[] bytes, final int offset) {
        return (bytes[offset] & 0xFF) << 8 | bytes[offset + 1] & 0xFF;
    }

    private static int readInt(final byte[] bytes, final int offset) {
        return readUnsignedShort(bytes, offset) << 16 | readUnsignedShort(bytes, offset + 2);
    }
}
