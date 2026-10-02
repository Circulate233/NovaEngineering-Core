package github.kasuminova.novaeng.common.integration;

import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyStorageCore;
import com.circulation.circulation_networks.events.BlockEntityLifeCycleEvent.Invalidate;
import com.circulation.circulation_networks.events.BlockEntityLifeCycleEvent.Validate;
import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class DECoreBindingIndex {

    private static final Reference2ObjectMap<World, LongOpenHashSet> ACTIVE_CORES = new Reference2ObjectOpenHashMap<>();
    private static boolean registered;

    private DECoreBindingIndex() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    public static void updateCoreState(final TileEnergyStorageCore core) {
        final World world = core.getWorld();
        if (!(world instanceof WorldServer serverWorld) || world.isRemote) {
            return;
        }
        executeOnServerThread(serverWorld, () -> {
            if (getLoadedTile(serverWorld, core.getPos()) != core || core.isInvalid()) {
                return;
            }
            updateCore(serverWorld, core.getPos(), core.active.value);
        });
    }

    private static void executeOnServerThread(final WorldServer world, final Runnable task) {
        if (world.isCallingFromMinecraftThread()) {
            task.run();
        } else {
            world.addScheduledTask(task);
        }
    }

    static void onValidate(final WorldServer world, final BlockPos pos, final TileEntity tile) {
        executeOnServerThread(world, () -> {
            if (getLoadedTile(world, pos) != tile) {
                return;
            }
            if (tile instanceof TileEnergyStorageCore core) {
                updateCore(world, pos, !core.isInvalid() && core.active.value);
            } else if (tile instanceof TileEnergyHatch hatch) {
                bindNearest(world, hatch);
            }
        });
    }

    static void onInvalidate(final WorldServer world, final BlockPos pos, final TileEntity tile) {
        executeOnServerThread(world, () -> {
            final TileEntity current = getLoadedTile(world, pos);
            if (current != null && current != tile) {
                return;
            }
            if (tile instanceof TileEnergyStorageCore) {
                updateCore(world, pos, false);
            } else if (tile instanceof TileEnergyHatch hatch) {
                ((TileEnergyHatchBinding) hatch).novaeng$setFoundCore(null);
            }
        });
    }

    private static void updateCore(final WorldServer world, final BlockPos pos, final boolean active) {
        LongOpenHashSet cores = ACTIVE_CORES.get(world);
        final boolean changed;
        if (active) {
            if (cores == null) {
                cores = new LongOpenHashSet();
                ACTIVE_CORES.put(world, cores);
            }
            changed = cores.add(pos.toLong());
        } else {
            changed = cores != null && cores.remove(pos.toLong());
        }
        if (changed) {
            rebindNearbyHatches(world, pos);
        }
        if (cores != null && cores.isEmpty()) {
            ACTIVE_CORES.remove(world);
        }
    }

    private static void rebindNearbyHatches(final WorldServer world, final BlockPos corePos) {
        final int range = EnergyHatchData.searchRange;
        final int minChunkX = (corePos.getX() - range) >> 4;
        final int maxChunkX = (corePos.getX() + range) >> 4;
        final int minChunkZ = (corePos.getZ() - range) >> 4;
        final int maxChunkZ = (corePos.getZ() + range) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                final Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (TileEntity tile : chunk.getTileEntityMap().values()) {
                    if (tile instanceof TileEnergyHatch hatch && withinRange(hatch.getPos(), corePos, range)) {
                        bindNearest(world, hatch);
                    }
                }
            }
        }
    }

    private static void bindNearest(final WorldServer world, final TileEnergyHatch hatch) {
        if (hatch.isInvalid() || getLoadedTile(world, hatch.getPos()) != hatch) {
            return;
        }
        final LongOpenHashSet cores = ACTIVE_CORES.get(world);
        if (cores == null || cores.isEmpty()) {
            ((TileEnergyHatchBinding) hatch).novaeng$setFoundCore(null);
            return;
        }

        final int range = EnergyHatchData.searchRange;
        BlockPos best = null;
        final LongIterator iterator = cores.iterator();
        while (iterator.hasNext()) {
            final BlockPos candidate = BlockPos.fromLong(iterator.nextLong());
            if (!withinRange(hatch.getPos(), candidate, range)) {
                continue;
            }
            final TileEntity tile = getLoadedTile(world, candidate);
            if (!(tile instanceof TileEnergyStorageCore core) || core.isInvalid() || !core.active.value) {
                iterator.remove();
                continue;
            }
            if (best == null || vanillaBoxOrderBefore(candidate, best)) {
                best = candidate;
            }
        }
        ((TileEnergyHatchBinding) hatch).novaeng$setFoundCore(best);
        if (cores.isEmpty()) {
            ACTIVE_CORES.remove(world);
        }
    }

    private static boolean vanillaBoxOrderBefore(final BlockPos candidate, final BlockPos current) {
        if (candidate.getZ() != current.getZ()) {
            return candidate.getZ() < current.getZ();
        }
        if (candidate.getY() != current.getY()) {
            return candidate.getY() < current.getY();
        }
        return candidate.getX() < current.getX();
    }

    private static boolean withinRange(final BlockPos a, final BlockPos b, final int range) {
        return Math.abs((long) a.getX() - b.getX()) <= range
            && Math.abs((long) a.getY() - b.getY()) <= range
            && Math.abs((long) a.getZ() - b.getZ()) <= range;
    }

    private static TileEntity getLoadedTile(final WorldServer world, final BlockPos pos) {
        final Chunk chunk = world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getTileEntityMap().get(pos);
    }

    public interface TileEnergyHatchBinding {
        void novaeng$setFoundCore(BlockPos pos);
    }

    public static final class Events {
        @SubscribeEvent
        public void onValidate(final Validate event) {
            if (event.getWorld() instanceof WorldServer world && !world.isRemote) {
                DECoreBindingIndex.onValidate(world, event.getPos(), event.getBlockEntity());
            }
        }

        @SubscribeEvent
        public void onInvalidate(final Invalidate event) {
            if (event.getWorld() instanceof WorldServer world && !world.isRemote) {
                DECoreBindingIndex.onInvalidate(world, event.getPos(), event.getBlockEntity());
            }
        }

        @SubscribeEvent
        public void onWorldUnload(final WorldEvent.Unload event) {
            ACTIVE_CORES.remove(event.getWorld());
        }
    }
}
