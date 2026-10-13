package github.kasuminova.novaeng.common.performance;

import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ChunkNbtRetentionTest {
    private static void forgetSource(final ChunkNbtRetention pending) throws Exception {
        // Deterministic simulation of GC; no timing-dependent System.gc assertions.
        final var reference = ChunkNbtRetention.class.getDeclaredField("original");
        reference.setAccessible(true);
        ((WeakReference<?>) reference.get(pending)).clear();
    }

    private static NBTTagCompound chunk(final boolean noise) {
        final NBTTagCompound root = new NBTTagCompound();
        final NBTTagCompound level = new NBTTagCompound();
        final NBTTagList sections = new NBTTagList();
        final Random random = new Random(42);
        for (int i = 0; i < 16; i++) {
            final NBTTagCompound section = new NBTTagCompound();
            section.setByte("Y", (byte) i);
            for (String key : new String[]{"Blocks", "Data", "Add", "Add2"}) {
                final byte[] bytes = new byte[key.equals("Blocks") ? 4096 : 2048];
                if (noise) {
                    random.nextBytes(bytes);
                } else {
                    for (int j = 0; j < bytes.length; j++) {
                        bytes[j] = (byte) (j / 256 + i);
                    }
                }
                section.setByteArray(key, bytes);
            }
            section.setIntArray("Palette", new int[]{0, 1, 65536, 1234567});
            section.setByteArray("BlockLight", new byte[2048]);
            section.setByteArray("SkyLight", new byte[2048]);
            section.setTag("modSection", new NBTTagCompound());
            sections.appendTag(section);
        }
        level.setTag("Sections", sections);
        level.setInteger("xPos", -17);
        level.setInteger("zPos", 23);
        level.setLong("LastUpdate", 987654321L);
        level.setByteArray("Biomes", new byte[256]);
        level.setIntArray("HeightMap", new int[256]);
        for (String key : new String[]{"Entities", "TileEntities", "TileTicks"}) {
            final NBTTagList list = new NBTTagList();
            final NBTTagCompound entry = new NBTTagCompound();
            entry.setString("id", "test:" + key);
            list.appendTag(entry);
            level.setTag(key, list);
        }
        level.setTag("ForgeCaps", new NBTTagCompound());
        root.setTag("modRoot", new NBTTagCompound());
        root.setTag("Level", level);
        root.setInteger("DataVersion", 1343);
        return root;
    }

    @Test
    void restoresEveryTagAfterTheOriginalIsCollected() throws Exception {
        final NBTTagCompound original = chunk(false);
        final NBTTagCompound expected = original.copy();
        final ChunkNbtRetention pending = ChunkNbtRetention.capture(original);
        assertNotNull(pending);
        final NBTTagCompound kept = pending.retainedData().getCompoundTag("Level");
        assertFalse(kept.getTagList("Sections", 10).getCompoundTagAt(0).hasKey("Blocks"));
        assertEquals(expected, original, "The pending-save input must never be changed");
        assertSame(original.getTag("modRoot"), pending.retainedData().getTag("modRoot"));
        assertSame(original.getCompoundTag("Level").getTag("Entities"), kept.getTag("Entities"));
        assertSame(original.getCompoundTag("Level").getTag("TileEntities"), kept.getTag("TileEntities"));
        assertSame(original.getCompoundTag("Level").getTag("TileTicks"), kept.getTag("TileTicks"));
        assertSame(original.getCompoundTag("Level").getTag("ForgeCaps"), kept.getTag("ForgeCaps"));
        final NBTTagCompound sourceSection = original.getCompoundTag("Level").getTagList("Sections", 10).getCompoundTagAt(0);
        final NBTTagCompound keptSection = kept.getTagList("Sections", 10).getCompoundTagAt(0);
        assertSame(sourceSection.getTag("BlockLight"), keptSection.getTag("BlockLight"));
        assertSame(sourceSection.getTag("modSection"), keptSection.getTag("modSection"));
        forgetSource(pending);
        final NBTTagCompound restored = pending.restore();
        assertEquals(expected, restored);
        assertSame(restored, pending.restore(), "Restoration is idempotent");
        final var encoded = ChunkNbtRetention.class.getDeclaredField("encoded");
        encoded.setAccessible(true);
        assertNull(encoded.get(pending), "Release compressed bytes after restoration");
    }

    @Test
    void livePendingSaveKeepsItsIdentityAndChanges() {
        final NBTTagCompound original = chunk(false);
        final ChunkNbtRetention pending = ChunkNbtRetention.capture(original);
        assertNotNull(pending);
        original.setInteger("modMutation", 123);
        assertSame(original, pending.restore());
        assertEquals(123, pending.restore().getInteger("modMutation"));
    }

    @Test
    void preservesCustomArraySubclassesWithoutSerializingThem() throws Exception {
        final NBTTagCompound original = chunk(false);
        final NBTTagByteArray custom = new NBTTagByteArray(new byte[4096]) {
        };
        original.getCompoundTag("Level").getTagList("Sections", 10).getCompoundTagAt(0).setTag("Blocks", custom);
        final ChunkNbtRetention pending = ChunkNbtRetention.capture(original);
        assertNotNull(pending);
        forgetSource(pending);
        assertSame(custom, pending.restore().getCompoundTag("Level").getTagList("Sections", 10).getCompoundTagAt(0).getTag("Blocks"));
    }

    @Test
    void keepsTheOriginalForSmallOrIncompressiblePayloads() {
        final NBTTagCompound small = new NBTTagCompound();
        assertNull(ChunkNbtRetention.capture(null));
        assertNull(ChunkNbtRetention.capture(small));
        final NBTTagCompound noisy = chunk(true);
        final NBTTagCompound expected = noisy.copy();
        assertNull(ChunkNbtRetention.capture(noisy));
        assertEquals(expected, noisy);
    }

    @Test
    void canCaptureOnAWorkerAndRestoreOnTheCallbackThread() throws Exception {
        final NBTTagCompound original = chunk(false);
        final NBTTagCompound expected = original.copy();
        final ChunkNbtRetention pending = CompletableFuture.supplyAsync(() -> ChunkNbtRetention.capture(original)).join();
        assertNotNull(pending);
        forgetSource(pending);
        assertEquals(expected, pending.restore());
    }
}
