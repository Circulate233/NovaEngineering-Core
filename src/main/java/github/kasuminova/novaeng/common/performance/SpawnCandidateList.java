package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.AbstractObjectList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.RandomAccess;

/**
 * Independent event lists share an immutable snapshot until a listener modifies its own list.
 */
public final class SpawnCandidateList extends AbstractObjectList<Biome.SpawnListEntry> implements RandomAccess {
    private static final Biome.SpawnListEntry[] EMPTY = new Biome.SpawnListEntry[0];
    private static final ThreadLocal<Biome.SpawnListEntry[]> LAST = new ThreadLocal<>();
    private final Biome.SpawnListEntry[] snapshot;
    private ObjectArrayList<Biome.SpawnListEntry> edited;

    private SpawnCandidateList(final Biome.SpawnListEntry[] snapshot) {
        this.snapshot = snapshot;
    }

    public static SpawnCandidateList copyOf(final List<Biome.SpawnListEntry> source) {
        if (source == null || source.isEmpty()) return new SpawnCandidateList(EMPTY);
        Biome.SpawnListEntry[] snapshot = LAST.get();
        boolean same = snapshot != null && source.size() == snapshot.length;
        if (same) {
            for (int index = 0; index < snapshot.length; index++) {
                if (source.get(index) != snapshot[index]) {
                    same = false;
                    break;
                }
            }
        }
        if (!same) {
            snapshot = source.toArray(new Biome.SpawnListEntry[0]);
            // Avoid retaining unusually large mod-generated lists on a thread.
            if (snapshot.length <= 256) LAST.set(snapshot);
            else LAST.remove();
        }
        return new SpawnCandidateList(snapshot);
    }

    public static void clearThreadCache() {
        LAST.remove();
    }

    private ObjectArrayList<Biome.SpawnListEntry> mutable() {
        if (edited == null) edited = new ObjectArrayList<>(snapshot);
        return edited;
    }

    @Override
    public int size() {
        return edited == null ? snapshot.length : edited.size();
    }

    @Override
    public Biome.SpawnListEntry get(final int index) {
        ensureRestrictedIndex(index);
        return edited == null ? snapshot[index] : edited.get(index);
    }

    @Override
    public Biome.SpawnListEntry set(final int index, final Biome.SpawnListEntry entry) {
        ensureRestrictedIndex(index);
        return mutable().set(index, entry);
    }

    @Override
    public void add(final int index, final Biome.SpawnListEntry entry) {
        ensureIndex(index);
        mutable().add(index, entry);
    }

    @Override
    public Biome.SpawnListEntry remove(final int index) {
        ensureRestrictedIndex(index);
        return mutable().remove(index);
    }

    @Override
    public void clear() {
        if (edited == null) edited = new ObjectArrayList<>(0);
        else edited.clear();
    }
}
