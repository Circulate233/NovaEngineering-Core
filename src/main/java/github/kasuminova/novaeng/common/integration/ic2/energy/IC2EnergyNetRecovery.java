package github.kasuminova.novaeng.common.integration.ic2.energy;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.mixin.ic2.AccessorEnergyNetDelegate;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.energy.tile.IMetaDelegate;
import ic2.core.block.comp.Energy;
import ic2.core.energy.grid.EnergyNetLocal;
import ic2.core.energy.grid.Tile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * World inspection happens in prepareSync; the original IC2 worker removes stale nodes later.
 */
public final class IC2EnergyNetRecovery {
    private static final ThreadLocal<Set<Object>> NOTIFICATION_EVENTS = new ThreadLocal<>();

    private IC2EnergyNetRecovery() {
    }

    /**
     * Returns false only for a proven obsolete addition, never for an unknown custom delegate.
     */
    public static boolean prepare(EnergyNetLocal enet, IEnergyTile incoming, List<IEnergyTile> subTiles) {
        EnergyNetRecoveryAccess access = (EnergyNetRecoveryAccess) enet;
        Map<IEnergyTile, List<StaleNode>> repairs = access.nova$getRepairs();
        repairs.remove(incoming);
        if (subTiles == null) return true; // REMOVAL has no sub-tile snapshot.

        World world = enet.getWorld();
        if (!(world instanceof WorldServer) || !((WorldServer) world).isCallingFromMinecraftThread()) return true;
        Status incomingStatus = status(incoming, world);
        if (incomingStatus == Status.STALE || incomingStatus == Status.UNLOADED) return false;
        if (incomingStatus != Status.CURRENT) return true;

        Map<BlockPos, Tile> registered = access.nova$getRegisteredTiles();
        Set<Tile> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<StaleNode> stale = new ObjectArrayList<>();
        for (IEnergyTile subTile : subTiles) {
            BlockPos subPos = EnergyNet.instance.getPos(subTile);
            Tile previous = registered.get(subPos);
            if (previous == null || previous.getMainTile() == incoming || !seen.add(previous)) continue;
            IEnergyTile previousIo = previous.getMainTile();
            // A live or unidentifiable conflict must retain the original IC2 rejection behavior.
            if (status(previousIo, world) != Status.STALE) return true;
            stale.add(new StaleNode(previousIo, EnergyNet.instance.getPos(previousIo), subPos, previous));
        }
        if (!stale.isEmpty()) repairs.put(incoming, stale);
        return true;
    }

    public static List<StaleNode> takeRepairs(EnergyNetLocal enet, IEnergyTile incoming) {
        EnergyNetRecoveryAccess access = (EnergyNetRecoveryAccess) enet;
        List<StaleNode> stale = access.nova$getRepairs().remove(incoming);
        if (stale == null) return Collections.emptyList();
        // Do not act on an outdated plan if another addition already replaced the captured node.
        for (StaleNode node : stale) {
            if (access.nova$getRegisteredTiles().get(node.conflictPosition) != node.registeredTile
                || node.registeredTile.getMainTile() != node.ioTile) return Collections.emptyList();
        }
        return stale;
    }

    /**
     * Refresh external EU handler caches after the original worker finishes registering the replacement.
     */
    public static void notifyIntegrations(final EnergyNetLocal enet, final IEnergyTile incoming, final List<StaleNode> stale) {
        final World world = enet.getWorld();
        if (!(world instanceof WorldServer) || stale.isEmpty()) return;
        ((WorldServer) world).addScheduledTask(() -> {
            if (status(incoming, world) != Status.CURRENT) return;
            // This call waits for an in-flight change step on the main thread. The worker never waits on us.
            Tile registered = enet.getTile(EnergyNet.instance.getPos(incoming));
            if (registered == null || registered.getMainTile() != incoming) return;
            Set<Object> events = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<Object> previousEvents = NOTIFICATION_EVENTS.get();
            NOTIFICATION_EVENTS.set(events);
            try {
                for (StaleNode node : stale) {
                    postNotification(new EnergyTileUnloadEvent(node.ioTile), events);
                }
                postNotification(new EnergyTileLoadEvent(incoming), events);
            } finally {
                if (previousEvents == null) NOTIFICATION_EVENTS.remove();
                else NOTIFICATION_EVENTS.set(previousEvents);
            }
        });
    }

    private static void postNotification(Event event, Set<Object> events) {
        events.add(event);
        try {
            MinecraftForge.EVENT_BUS.post(event);
        } catch (RuntimeException failure) {
            // A broken unload listener must not prevent the replacement load notification.
            NovaEngineeringCore.log.error("Energy node recovered, but an external EU integration failed to handle {}", event.getClass().getSimpleName(), failure);
        }
    }

    public static boolean isNotificationEvent(Object event) {
        Set<Object> events = NOTIFICATION_EVENTS.get();
        return events != null && events.contains(event);
    }

    private static Status status(IEnergyTile tile, World world) {
        if (tile instanceof IMetaDelegate) return Status.UNKNOWN;
        TileEntity owner;
        Energy energy = null;
        if (tile instanceof AccessorEnergyNetDelegate) {
            energy = ((AccessorEnergyNetDelegate) tile).nova$getEnergy();
            owner = energy.getParent();
        } else if (tile instanceof TileEntity) {
            owner = (TileEntity) tile;
            // Third-party helpers can be TileEntity objects without being the block entity stored
            // in the world. Preserve those unidentifiable wrappers.
            if (TileEntity.getKey(owner.getClass()) == null) return Status.UNKNOWN;
        } else {
            return Status.UNKNOWN;
        }
        if (owner.getWorld() != world || owner.isInvalid()) return Status.STALE;
        BlockPos ownerPos = owner.getPos();
        Chunk chunk = ((WorldServer) world).getChunkProvider().getLoadedChunk(ownerPos.getX() >> 4, ownerPos.getZ() >> 4);
        if (chunk == null) return Status.UNLOADED;
        TileEntity current = chunk.getTileEntity(ownerPos, Chunk.EnumCreateEntityType.CHECK);
        if (current != owner) return Status.STALE;
        if (energy != null && energy.getDelegate() != tile) return Status.STALE;
        return Status.CURRENT;
    }

    private enum Status {CURRENT, STALE, UNKNOWN, UNLOADED}

    public record StaleNode(IEnergyTile ioTile, BlockPos position, BlockPos conflictPosition, Tile registeredTile) {
        public StaleNode(IEnergyTile ioTile, BlockPos position, BlockPos conflictPosition, Tile registeredTile) {
            this.ioTile = ioTile;
            this.position = new BlockPos(position);
            this.conflictPosition = new BlockPos(conflictPosition);
            this.registeredTile = registeredTile;
        }
    }
}
