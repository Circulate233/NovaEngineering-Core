package github.kasuminova.novaeng.common.handler;

import github.kasuminova.novaeng.common.hypernet.old.HyperNetCaches;
import github.kasuminova.novaeng.common.machine.DreamEnergyCore;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Drops the static caches that are keyed by a {@link World}, or by a tile entity that
 * holds one, when that world goes away.
 *
 * <p>Those caches cannot expire on their own. A {@code World} reached from a static map
 * keeps every chunk, entity and tile entity it still holds alive, so one stale entry
 * costs as much memory as the world was using when it unloaded - and on the client,
 * where a {@code WorldClient} is retained per world the player ever joined, that is the
 * render distance's worth of chunks each time.</p>
 *
 * <p>This runs on both sides: {@code WorldEvent.Unload} fires for a {@code WorldServer}
 * when its dimension unloads, and for a {@code WorldClient} when the player disconnects
 * or changes dimension.</p>
 */
public final class WorldCacheCleanupHandler {

    public static final WorldCacheCleanupHandler INSTANCE = new WorldCacheCleanupHandler();

    private WorldCacheCleanupHandler() {
    }

    @SubscribeEvent
    public void onWorldUnload(final WorldEvent.Unload event) {
        final World world = event.getWorld();
        if (world == null) {
            return;
        }
        HyperNetCaches.removeWorld(world);
        DreamEnergyCore.removeWorld(world);
    }
}
