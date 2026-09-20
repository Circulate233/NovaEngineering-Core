package github.kasuminova.novaeng.client.texture;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable array-backed implementation of {@link SlotCapacityFrontier}.
 *
 * <p>Child frontiers are merged by sorting rectangles from widest to narrowest and retaining only
 * strictly increasing heights. The result is the exact Pareto frontier for containment queries.</p>
 */
public final class SlotCapacityFrontierImpl implements SlotCapacityFrontier {

    private static final SlotCapacityFrontier EMPTY = new SlotCapacityFrontierImpl(new int[0], new int[0]);

    private final int[] widths;
    private final int[] heights;

    private SlotCapacityFrontierImpl(final int[] widths, final int[] heights) {
        this.widths = widths;
        this.heights = heights;
    }

    /**
     * Creates the frontier for an occupied subtree, which has no free leaf.
     *
     * @return the shared empty frontier
     */
    public static SlotCapacityFrontier empty() {
        return EMPTY;
    }

    /**
     * Creates the frontier for one unoccupied, unsplit slot.
     *
     * @param width slot width
     * @param height slot height
     * @return a frontier containing the slot when both dimensions are positive, otherwise empty
     */
    public static SlotCapacityFrontier freeRectangle(final int width, final int height) {
        if (width <= 0 || height <= 0) {
            return EMPTY;
        }
        return new SlotCapacityFrontierImpl(new int[]{width}, new int[]{height});
    }

    /**
     * Merges immediate child frontiers and removes only rectangles dominated by another child leaf.
     *
     * @param children current frontiers of the slot's immediate children
     * @return the exact non-dominated union of all child capacities
     */
    public static SlotCapacityFrontier merge(final List<? extends SlotCapacityFrontier> children) {
        int capacityCount = 0;
        for (final SlotCapacityFrontier child : children) {
            capacityCount += child.size();
        }
        if (capacityCount == 0) {
            return EMPTY;
        }

        final List<Rectangle> rectangles = new ArrayList<>(capacityCount);
        for (final SlotCapacityFrontier child : children) {
            for (int i = 0; i < child.size(); i++) {
                rectangles.add(new Rectangle(child.widthAt(i), child.heightAt(i)));
            }
        }
        rectangles.sort(Comparator.comparingInt(Rectangle::width).reversed()
            .thenComparing(Comparator.comparingInt(Rectangle::height).reversed()));

        final int[] mergedWidths = new int[rectangles.size()];
        final int[] mergedHeights = new int[rectangles.size()];
        int mergedSize = 0;
        int greatestHeight = -1;
        for (final Rectangle rectangle : rectangles) {
            if (rectangle.height() <= greatestHeight) {
                continue;
            }
            mergedWidths[mergedSize] = rectangle.width();
            mergedHeights[mergedSize] = rectangle.height();
            mergedSize++;
            greatestHeight = rectangle.height();
        }

        if (mergedSize == rectangles.size()) {
            return new SlotCapacityFrontierImpl(mergedWidths, mergedHeights);
        }
        final int[] compactWidths = new int[mergedSize];
        final int[] compactHeights = new int[mergedSize];
        System.arraycopy(mergedWidths, 0, compactWidths, 0, mergedSize);
        System.arraycopy(mergedHeights, 0, compactHeights, 0, mergedSize);
        return new SlotCapacityFrontierImpl(compactWidths, compactHeights);
    }

    @Override
    public boolean canFit(final int width, final int height) {
        for (int i = 0; i < this.widths.length; i++) {
            if (width <= this.widths[i] && height <= this.heights[i]) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return this.widths.length;
    }

    @Override
    public int widthAt(final int index) {
        return this.widths[index];
    }

    @Override
    public int heightAt(final int index) {
        return this.heights[index];
    }

    private record Rectangle(int width, int height) {
    }
}
