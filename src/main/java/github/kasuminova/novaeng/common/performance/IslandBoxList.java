package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.AbstractObjectList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.List;
import java.util.RandomAccess;

/**
 * Live list with positive spatial hints. Bounds remain live and misses always scan.
 */
public final class IslandBoxList extends AbstractObjectList<StructureBoundingBox> implements RandomAccess {
    private final List<StructureBoundingBox> boxes;
    private final Long2ObjectLinkedOpenHashMap<StructureBoundingBox> hints = new Long2ObjectLinkedOpenHashMap<>();
    private int customBoxes;

    public IslandBoxList(final List<StructureBoundingBox> boxes) {
        this.boxes = boxes;
        for (final StructureBoundingBox box : boxes) customBoxes += custom(box);
    }

    private static int custom(final StructureBoundingBox box) {
        return box != null && box.getClass() == StructureBoundingBox.class ? 0 : 1;
    }

    public boolean containsPosition(final BlockPos pos) {
        final long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        if (customBoxes == 0) {
            final StructureBoundingBox hint = hints.getAndMoveToFirst(key);
            if (hint != null && hint.isVecInside(pos)) return true;
        }
        for (final StructureBoundingBox box : boxes) {
            if (!box.isVecInside(pos)) continue;
            if (customBoxes == 0) {
                hints.putAndMoveToFirst(key, box);
                if (hints.size() > 256) hints.removeLast();
            }
            return true;
        }
        return false;
    }

    @Override
    public StructureBoundingBox get(final int index) {
        return boxes.get(index);
    }

    @Override
    public int size() {
        return boxes.size();
    }

    @Override
    public void add(final int index, final StructureBoundingBox box) {
        boxes.add(index, box);
        customBoxes += custom(box);
        hints.clear();
    }

    @Override
    public StructureBoundingBox set(final int index, final StructureBoundingBox box) {
        final StructureBoundingBox previous = boxes.set(index, box);
        customBoxes += custom(box) - custom(previous);
        hints.clear();
        return previous;
    }

    @Override
    public StructureBoundingBox remove(final int index) {
        final StructureBoundingBox previous = boxes.remove(index);
        customBoxes -= custom(previous);
        hints.clear();
        return previous;
    }

    @Override
    public void clear() {
        boxes.clear();
        customBoxes = 0;
        hints.clear();
    }
}
