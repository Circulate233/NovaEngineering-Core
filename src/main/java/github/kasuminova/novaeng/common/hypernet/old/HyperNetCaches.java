package github.kasuminova.novaeng.common.hypernet.old;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.world.World;

/**
 * The one place that drops the HyperNet caches keyed by a machine controller.
 *
 * <p>{@link NetNodeCache} and {@link ComputationCenter} both map a
 * {@link TileMultiblockMachineController} to an object that holds that same controller
 * back. Neither can expire on its own: the tile does not override equals/hashCode, so
 * the maps compare by identity and a reloaded chunk produces a fresh key every time,
 * and the value's own reference to the key defeats any weak-reference scheme. A tile
 * left in either map keeps its {@code world} field alive, and with it every chunk,
 * entity and tile entity that world still holds - on the client that is a whole
 * {@code WorldClient} per world the player ever visited.</p>
 *
 * <p>So the entries are removed explicitly, from the tile's own lifecycle (see the
 * {@code MixinTileMultiblockMachineControllerCache} mixin) with a per-world sweep as a
 * backstop for tiles that never get that far.</p>
 */
public final class HyperNetCaches {

    private HyperNetCaches() {
    }

    /** Forgets one controller. Called when the tile invalidates or its chunk unloads. */
    public static void remove(final TileMultiblockMachineController ctrl) {
        NetNodeCache.removeCache(ctrl);
        ComputationCenter.removeCache(ctrl);
    }

    /** Forgets every controller in {@code world}. Called when the world unloads. */
    public static void removeWorld(final World world) {
        NetNodeCache.removeWorld(world);
        ComputationCenter.removeWorld(world);
    }

    /** Forgets everything. Called when the client leaves a world entirely. */
    public static void clear() {
        NetNodeCache.clearCache();
        ComputationCenter.clearCache();
    }
}
