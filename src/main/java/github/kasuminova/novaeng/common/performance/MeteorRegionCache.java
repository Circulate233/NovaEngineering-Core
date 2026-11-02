package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

/**
 * Owned by one SpawnData instance and used under its existing class monitor.
 */
public final class MeteorRegionCache {
    private static final int CAPACITY = 128;
    private static final long RETRY_DELAY_NANOS = 2_000_000_000L;
    private static final Logger LOGGER = LogManager.getLogger("NovaEngCore/MeteorCache");
    private final Object2ObjectLinkedOpenHashMap<File, Entry> entries = new Object2ObjectLinkedOpenHashMap<>();
    private final Object2ObjectOpenHashMap<File, NBTTagCompound> pending = new Object2ObjectOpenHashMap<>();
    private final Object monitor;
    private Object2LongOpenHashMap<File> retryAfter;
    private File readingFile;
    private BasicFileAttributes readingVersion;
    private boolean failureReported;

    public MeteorRegionCache() {
        this(new Object());
    }

    public MeteorRegionCache(final Object monitor) {
        this.monitor = monitor;
    }

    private static BasicFileAttributes attributes(final File file) {
        try {
            final BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            return attributes.isRegularFile() ? attributes : null;
        } catch (final IOException | SecurityException ignored) {
            return null;
        }
    }

    public NBTTagCompound get(final File file) {
        final NBTTagCompound dirty = pending.get(file);
        if (dirty != null) return dirty.copy();
        final Entry entry = current(file);
        if (entry != null) return entry.data.copy();
        readingFile = file;
        readingVersion = attributes(file);
        return null;
    }

    /**
     * Cache only a version that stayed unchanged between the pre-open probe and the completed read.
     */
    public void completeRead(final File file, final NBTTagCompound data) {
        if (readingFile != file) return;
        final BasicFileAttributes before = readingVersion;
        readingFile = null;
        readingVersion = null;
        final BasicFileAttributes after = attributes(file);
        if (before != null && after != null && before.size() == after.size()
            && before.lastModifiedTime().equals(after.lastModifiedTime())
            && Objects.equals(before.fileKey(), after.fileKey())) {
            putOwned(file, data.copy(), after);
        }
    }

    public boolean containsUnchanged(final File file, final NBTTagCompound data) {
        final NBTTagCompound dirty = pending.get(file);
        if (dirty != null) return dirty.equals(data);
        final Entry entry = current(file);
        return entry != null && entry.data.equals(data);
    }

    private Entry current(final File file) {
        final Entry entry = entries.getAndMoveToFirst(file);
        if (entry == null) return null;
        final BasicFileAttributes attributes = attributes(file);
        if (attributes == null || entry.size != attributes.size()
            || !entry.modified.equals(attributes.lastModifiedTime())
            || !Objects.equals(entry.fileKey, attributes.fileKey())) {
            entries.remove(file);
            return null;
        }
        return entry;
    }

    public boolean stage(final File file, final NBTTagCompound data) {
        if (!MeteorWriteBatch.active()) return false;
        if (pending.size() >= CAPACITY && !pending.containsKey(file)) {
            flushPending(false);
            // Bound retained failed writes. Additional regions are written synchronously.
            if (pending.size() >= CAPACITY) return false;
        }
        pending.put(file, data.copy());
        entries.remove(file);
        MeteorWriteBatch.dirty(this);
        return true;
    }

    public void flushPending() {
        flushPending(true);
    }

    public void flushPending(final boolean force) {
        synchronized (monitor) {
            final var iterator = pending.object2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                final var entry = iterator.next();
                final File file = entry.getKey();
                final NBTTagCompound data = entry.getValue();
                if (!force && retryAfter != null && retryAfter.containsKey(file)
                    && System.nanoTime() - retryAfter.getLong(file) < 0) continue;
                try {
                    final BasicFileAttributes version = MeteorRegionFile.write(file, data);
                    putOwned(file, data, version);
                    if (retryAfter != null) retryAfter.removeLong(file);
                    iterator.remove();
                } catch (final IOException | RuntimeException failure) {
                    recordFailure(file, failure);
                }
            }
            clearDirtyIfEmpty();
        }
    }

    /**
     * Called under SpawnData's monitor. A failed retry must not erase the previously staged region.
     */
    public void writeThrough(final File file, final NBTTagCompound data) throws IOException {
        final NBTTagCompound snapshot = data.copy();
        entries.remove(file);
        final BasicFileAttributes version;
        try {
            version = MeteorRegionFile.write(file, snapshot);
        } catch (final IOException | RuntimeException failure) {
            if (pending.containsKey(file) || pending.size() < CAPACITY) {
                pending.put(file, snapshot);
                MeteorWriteBatch.dirty(this);
                recordFailure(file, failure);
            }
            throw failure;
        }
        pending.remove(file);
        if (retryAfter != null) retryAfter.removeLong(file);
        putOwned(file, snapshot, version);
        clearDirtyIfEmpty();
    }

    private void recordFailure(final File file, final Exception failure) {
        entries.remove(file);
        if (retryAfter == null) retryAfter = new Object2LongOpenHashMap<>();
        retryAfter.put(file, System.nanoTime() + RETRY_DELAY_NANOS);
        if (!failureReported) {
            LOGGER.error("Failed to save meteor region {}; retaining the pending data for retry", file, failure);
            failureReported = true;
        }
    }

    private void clearDirtyIfEmpty() {
        if (!pending.isEmpty()) return;
        failureReported = false;
        MeteorWriteBatch.clean(this);
    }

    public void put(final File file, final NBTTagCompound data) {
        putOwned(file, data.copy());
    }

    private void putOwned(final File file, final NBTTagCompound data) {
        putOwned(file, data, attributes(file));
    }

    private void putOwned(final File file, final NBTTagCompound data, final BasicFileAttributes attributes) {
        if (attributes == null) {
            entries.remove(file);
            return;
        }
        entries.putAndMoveToFirst(file, new Entry(data, attributes.size(),
            attributes.lastModifiedTime(), attributes.fileKey()));
        if (entries.size() > CAPACITY) entries.removeLast();
    }

    private record Entry(NBTTagCompound data, long size, FileTime modified, Object fileKey) {
    }
}
