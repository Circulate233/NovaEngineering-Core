package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Releases decoded section arrays while a Forge chunk waits for its synchronous phase.
 * Forge's load event still needs those bytes, so retain them losslessly instead of dropping tags.
 * Entity data, lighting arrays shared with the chunk, and arbitrary mod tags retain their references.
 * The compressed bytes are an in-memory field only; they are never attached to the NBT tree or passed
 * to AnvilChunkLoader's save queue. Every disk read therefore continues to accept the vanilla format,
 * including worlds written before this optimization.
 */
public final class ChunkNbtRetention {
    private static final String[] SECTION_ARRAYS = {"Blocks", "Data", "Add", "Add2", "Palette"};
    private static final int MIN_ARRAY_BYTES = 8192;
    private static final int ENCODED_MAGIC = 0x4E454E31;
    private static final int MAX_ARRAY_LENGTH = 1 << 24;

    private final WeakReference<NBTTagCompound> original;
    private final int encodedLength;
    private NBTTagCompound retained;
    /** Deferred in-memory section data; this is deliberately not an NBT tag. */
    private byte[] encoded;

    private ChunkNbtRetention(final NBTTagCompound original, final NBTTagCompound retained,
                              final byte[] encoded, final int encodedLength) {
        this.original = new WeakReference<>(original);
        this.retained = retained;
        this.encoded = encoded;
        this.encodedLength = encodedLength;
    }

    public static ChunkNbtRetention capture(final NBTTagCompound root) {
        if (root == null || root.getClass() != NBTTagCompound.class || !root.hasKey("Level", 10)) {
            return null;
        }
        final NBTTagCompound level = root.getCompoundTag("Level");
        if (level.getClass() != NBTTagCompound.class || !level.hasKey("Sections", 9)) {
            return null;
        }
        final NBTTagList sections = level.getTagList("Sections", 10);
        if (sections.getClass() != NBTTagList.class) {
            return null;
        }
        final int sectionCount = sections.tagCount();
        final int[] masks = new int[sectionCount];
        long arrayBytes = 0;
        for (int i = 0; i < sectionCount; i++) {
            final NBTTagCompound section = sections.getCompoundTagAt(i);
            if (section.getClass() == NBTTagCompound.class) {
                for (int j = 0; j < SECTION_ARRAYS.length; j++) {
                    final long bytes = arrayBytes(section.getTag(SECTION_ARRAYS[j]));
                    if (bytes != 0) {
                        masks[i] |= 1 << j;
                        arrayBytes += bytes;
                    }
                }
            }
        }
        if (arrayBytes < MIN_ARRAY_BYTES) {
            return null;
        }
        final NBTTagList keptSections = new NBTTagList();
        for (int i = 0; i < sectionCount; i++) {
            final NBTTagCompound section = sections.getCompoundTagAt(i);
            NBTTagCompound kept = section;
            if (section.getClass() == NBTTagCompound.class) {
                for (int j = 0; j < SECTION_ARRAYS.length; j++) {
                    if ((masks[i] & (1 << j)) != 0) {
                        if (kept == section) {
                            kept = shallowCopy(section);
                        }
                        kept.removeTag(SECTION_ARRAYS[j]);
                    }
                }
            }
            keptSections.appendTag(kept);
        }
        final FastByteArrayOutputStream buffer = new FastByteArrayOutputStream(512);
        final Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try (DataOutputStream output = new DataOutputStream(new DeflaterOutputStream(buffer, deflater))) {
            output.writeInt(ENCODED_MAGIC);
            output.writeInt(sectionCount);
            for (int i = 0; i < sectionCount; i++) {
                final int mask = masks[i];
                output.writeInt(mask);
                final NBTTagCompound section = sections.getCompoundTagAt(i);
                for (int j = 0; j < SECTION_ARRAYS.length; j++) {
                    if ((mask & (1 << j)) == 0) {
                        continue;
                    }
                    final NBTBase tag = section.getTag(SECTION_ARRAYS[j]);
                    if (tag.getClass() == NBTTagByteArray.class) {
                        output.writeByte(0);
                        final byte[] bytes = ((NBTTagByteArray) tag).getByteArray();
                        output.writeInt(bytes.length);
                        output.write(bytes);
                    } else {
                        output.writeByte(1);
                        final int[] values = ((NBTTagIntArray) tag).getIntArray();
                        output.writeInt(values.length);
                        for (int value : values) {
                            output.writeInt(value);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // No input tags were modified; the original Forge path is still usable.
            return null;
        } finally {
            deflater.end();
        }
        // Include buffer slack and the small compound shells in the savings decision.
        if (buffer.array.length + 1024L + sectionCount * 512L >= arrayBytes) {
            return null;
        }
        final NBTTagCompound keptLevel = shallowCopy(level);
        keptLevel.setTag("Sections", keptSections);
        final NBTTagCompound keptRoot = shallowCopy(root);
        keptRoot.setTag("Level", keptLevel);
        return new ChunkNbtRetention(root, keptRoot, buffer.array, buffer.length);
    }

    private static long arrayBytes(final NBTBase tag) {
        if (tag != null && tag.getClass() == NBTTagByteArray.class) {
            return ((NBTTagByteArray) tag).getByteArray().length;
        }
        if (tag != null && tag.getClass() == NBTTagIntArray.class) {
            return 4L * ((NBTTagIntArray) tag).getIntArray().length;
        }
        return 0;
    }

    private static NBTTagCompound shallowCopy(final NBTTagCompound source) {
        final NBTTagCompound result = new NBTTagCompound();
        for (String key : source.getKeySet()) {
            result.setTag(key, source.getTag(key));
        }
        return result;
    }

    public NBTTagCompound retainedData() {
        return retained;
    }

    public NBTTagCompound restore() {
        if (encoded == null) {
            return retained;
        }
        // A pending save or another mod may still own the source. Preserve its identity and changes.
        final NBTTagCompound liveOriginal = original.get();
        if (liveOriginal != null) {
            retained = liveOriginal;
        } else {
            final NBTTagList sections = retained.getCompoundTag("Level").getTagList("Sections", 10);
            try (DataInputStream input = new DataInputStream(new InflaterInputStream(
                new ByteArrayInputStream(encoded, 0, encodedLength)))) {
                if (input.readInt() != ENCODED_MAGIC) {
                    throw new IOException("Unknown deferred chunk section encoding");
                }
                final int sectionCount = input.readInt();
                if (sectionCount != sections.tagCount()) {
                    throw new IOException("Deferred chunk section count changed");
                }
                for (int i = 0; i < sectionCount; i++) {
                    final NBTTagCompound section = sections.getCompoundTagAt(i);
                    final int mask = input.readInt();
                    for (int j = 0; j < SECTION_ARRAYS.length; j++) {
                        if ((mask & (1 << j)) == 0) {
                            continue;
                        }
                        final int type = input.readUnsignedByte();
                        final int length = input.readInt();
                        if (length < 0 || length > MAX_ARRAY_LENGTH) {
                            throw new IOException("Invalid deferred chunk array length");
                        }
                        if (type == 0) {
                            final byte[] bytes = new byte[length];
                            input.readFully(bytes);
                            section.setByteArray(SECTION_ARRAYS[j], bytes);
                        } else if (type == 1) {
                            final int[] values = new int[length];
                            for (int k = 0; k < length; k++) {
                                values[k] = input.readInt();
                            }
                            section.setIntArray(SECTION_ARRAYS[j], values);
                        } else {
                            throw new IOException("Unknown deferred chunk array type");
                        }
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to restore deferred chunk section NBT", e);
            }
        }
        encoded = null;
        original.clear();
        return retained;
    }
}
