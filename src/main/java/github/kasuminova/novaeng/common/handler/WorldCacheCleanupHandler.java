package github.kasuminova.novaeng.common.handler;

import com.brandon3055.draconicevolution.blocks.tileentity.TileGrinder;
import github.kasuminova.novaeng.common.hypernet.old.HyperNetCaches;
import github.kasuminova.novaeng.common.machine.DreamEnergyCore;
import github.kasuminova.novaeng.common.machine.MMAltar;
import github.kasuminova.novaeng.common.performance.ClassResourceCacheCompactor;
import github.kasuminova.novaeng.common.performance.MeteorWriteBatch;
import github.kasuminova.novaeng.common.performance.SpawnCandidateList;
import github.kasuminova.novaeng.common.util.MixinDecisions;
import github.kasuminova.novaeng.mixin.rpsideas.AccessorItemBioticSensor;
import hellfirepvp.modularmachinery.common.base.Mods;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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
    public void flushMeteorWrites(final TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) MeteorWriteBatch.flushReady();
    }

    @SubscribeEvent
    public void flushMeteorWrites(final WorldEvent.Save event) {
        if (!event.getWorld().isRemote) MeteorWriteBatch.flushAll();
    }

    @SubscribeEvent
    public void onWorldUnload(final WorldEvent.Unload event) {
        final World world = event.getWorld();
        if (world == null) {
            return;
        }
        if (!world.isRemote) MeteorWriteBatch.flushAll();
        SpawnCandidateList.clearThreadCache();
        if (MixinDecisions.rpsIdeasLoaded) {
            final var biotics = world.isRemote ? AccessorItemBioticSensor.nova$remoteBiotics()
                : AccessorItemBioticSensor.nova$serverBiotics();
            final var players = biotics.entrySet().iterator();
            while (players.hasNext()) {
                final var entry = players.next();
                if (entry.getKey().world == world) {
                    players.remove();
                } else {
                    entry.getValue().removeIf(entityLivingBase -> entityLivingBase.world == world);
                }
            }
        }
        if (!world.isRemote && MixinDecisions.draconicEvolutionLoaded) {
            final var fakePlayer = TileGrinder.fakePlayer;
            if (fakePlayer != null && fakePlayer.world == world) TileGrinder.fakePlayer = null;
        }
        HyperNetCaches.removeWorld(world);
        DreamEnergyCore.removeWorld(world);
        if (Mods.BM2.isPresent()) {
            MMAltar.removeWorld(world);
        }
        ClassResourceCacheCompactor.compactIfGrown();
    }

    @SubscribeEvent
    public void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (MixinDecisions.rpsIdeasLoaded) {
            AccessorItemBioticSensor.nova$serverBiotics().remove(event.player);
        }
    }
}
