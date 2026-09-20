package github.kasuminova.novaeng.client.texture;

/**
 * Describes the exact non-dominated free rectangle capacities in a Stitcher slot subtree.
 *
 * <p>Each entry represents one leaf rectangle that the original recursive first-fit algorithm can
 * enter. An entry is omitted only when another entry is at least as wide and at least as tall, so
 * omission cannot change whether a holder can fit.</p>
 */
public interface SlotCapacityFrontier {

    /**
     * Tests whether at least one free leaf can contain the supplied dimensions.
     *
     * @param width required holder width in its current rotation
     * @param height required holder height in its current rotation
     * @return {@code true} when the original recursive search may find a fitting leaf
     */
    boolean canFit(int width, int height);

    /**
     * Returns the number of non-dominated rectangles retained by this frontier.
     *
     * @return frontier entry count
     */
    int size();

    /**
     * Returns one retained rectangle's width.
     *
     * @param index frontier entry index
     * @return free width
     */
    int widthAt(int index);

    /**
     * Returns one retained rectangle's height.
     *
     * @param index frontier entry index
     * @return free height
     */
    int heightAt(int index);
}
