package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.SpawnChunkCandidates;
import github.kasuminova.novaeng.common.performance.SpawnCountLimiter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

@Mixin(WorldEntitySpawner.class)
public abstract class MixinWorldEntitySpawner {

    @Unique
    private final EnumCreatureType[] nova$types = EnumCreatureType.values();
    @Unique
    private SpawnChunkCandidates nova$candidates;
    @Unique
    private Random nova$shuffleRandom;

    @Unique
    private static boolean nova$isWithinSpawnLimit(final WorldServer world, final EnumCreatureType type, final int limit) {
        return SpawnCountLimiter.isWithinLimit(world.loadedEntityList, entity -> entity.isCreatureType(type, true), limit);
    }

    @Unique
    private static void nova$randomChunkPosition(final World world, final int chunkX, final int chunkZ,
                                                 final BlockPos.MutableBlockPos pos) {
        final Chunk chunk = world.getChunk(chunkX, chunkZ);
        final int x = chunkX * 16 + world.rand.nextInt(16);
        final int z = chunkZ * 16 + world.rand.nextInt(16);
        pos.setPos(x, 0, z);
        final int height = MathHelper.roundUp(chunk.getHeight(pos) + 1, 16);
        final int y = world.rand.nextInt(height > 0 ? height : chunk.getTopFilledSegment() + 16 - 1);
        pos.setPos(x, y, z);
    }

    @Inject(method = "<init>()V", at = @At("RETURN"), require = 1)
    private void nova$initializeSpawnBuffers(final CallbackInfo ci) {
        this.nova$candidates = new SpawnChunkCandidates();
        this.nova$shuffleRandom = new Random();
    }

    /**
     * @author circulation
     * @reason Keep candidate coordinates and shuffle buffers primitive, and stop live entity counting once the cap is exceeded.
     */
    @Overwrite
    public int findChunksForSpawning(final WorldServer world, final boolean spawnHostileMobs,
                                     final boolean spawnPeacefulMobs, final boolean spawnOnSetTickRate) {
        if (!spawnHostileMobs && !spawnPeacefulMobs) {
            return 0;
        }

        final SpawnChunkCandidates candidates = this.nova$candidates;
        candidates.clear(world.playerEntities.size() > 1);
        final WorldBorder border = world.getWorldBorder();
        final PlayerChunkMap chunkMap = world.getPlayerChunkMap();
        final boolean defaultBorder = border.getClass() == WorldBorder.class;
        final SpawnChunkCandidates.ChunkFilter filter = (x, z) -> {
            if (defaultBorder) {
                if (!((x << 4) + 15 > border.minX() && (x << 4) < border.maxX()
                    && (z << 4) + 15 > border.minZ() && (z << 4) < border.maxZ())) {
                    return false;
                }
            } else if (!border.contains(new ChunkPos(x, z))) {
                return false;
            }
            final PlayerChunkMapEntry entry = chunkMap.getEntry(x, z);
            return entry != null && entry.isSentToPlayers();
        };
        for (final EntityPlayer player : world.playerEntities) {
            if (!player.isSpectator()) {
                candidates.addPlayer(MathHelper.floor(player.posX / 16.0), MathHelper.floor(player.posZ / 16.0), filter);
            }
        }

        int spawned = 0;
        final BlockPos spawnPoint = world.getSpawnPoint();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (final EnumCreatureType type : nova$types) {
            if ((type.getPeacefulCreature() && !spawnPeacefulMobs)
                || (!type.getPeacefulCreature() && !spawnHostileMobs)
                || (type.getAnimal() && !spawnOnSetTickRate)) {
                continue;
            }
            final int limit = type.getMaxNumberOfCreature() * candidates.capChunkCount() / 289;
            if (!nova$isWithinSpawnLimit(world, type, limit)) {
                continue;
            }

            final long[] chunks = candidates.shuffle(this.nova$shuffleRandom);
            chunkLoop:
            for (int chunkIndex = 0; chunkIndex < candidates.size(); chunkIndex++) {
                final long key = chunks[chunkIndex];
                nova$randomChunkPosition(world, (int) key, (int) (key >>> 32), pos);
                final int baseX = pos.getX();
                final int y = pos.getY();
                final int baseZ = pos.getZ();
                final IBlockState state = world.getBlockState(pos);
                if (state.isNormalCube()) {
                    continue;
                }

                int packSpawned = 0;
                for (int group = 0; group < 3; group++) {
                    int x = baseX;
                    int z = baseZ;
                    Biome.SpawnListEntry spawnEntry = null;
                    IEntityLivingData groupData = null;
                    List<Biome.SpawnListEntry> heldList = null;
                    long heldListPos = 0;
                    final int attempts = MathHelper.ceil(Math.random() * 4.0);
                    for (int attempt = 0; attempt < attempts; attempt++) {
                        x += world.rand.nextInt(6) - world.rand.nextInt(6);
                        // These zero-valued draws still advance the world's random sequence.
                        z += world.rand.nextInt(6) - world.rand.nextInt(6);
                        pos.setPos(x, y, z);
                        final float entityX = x + 0.5F;
                        final float entityZ = z + 0.5F;
                        if (world.isAnyPlayerWithinRangeAt(entityX, y, entityZ, 24.0)
                            || spawnPoint.distanceSq(entityX, y, entityZ) < 576.0) {
                            continue;
                        }

                        if (spawnEntry == null) {
                            heldList = ForgeEventFactory.getPotentialSpawns(world, type, pos,
                                world.getChunkProvider().getPossibleCreatures(type, pos));
                            heldListPos = pos.toLong();
                            if (heldList == null || heldList.isEmpty()) {
                                break;
                            }
                            spawnEntry = WeightedRandom.getRandomItem(world.rand, heldList);
                            if (spawnEntry == null) {
                                break;
                            }
                        }

                        // A pack retains its entry while the attempted position continues to move.
                        final boolean allowedHere = heldList != null && heldListPos == pos.toLong()
                            ? heldList.contains(spawnEntry) : world.canCreatureTypeSpawnHere(type, spawnEntry, pos);
                        if (!allowedHere || !WorldEntitySpawner.canCreatureTypeSpawnAtLocation(
                            EntitySpawnPlacementRegistry.getPlacementForEntity(spawnEntry.entityClass), world, pos)) {
                            continue;
                        }

                        final EntityLiving entity;
                        try {
                            entity = spawnEntry.newInstance(world);
                        } catch (final Exception exception) {
                            exception.printStackTrace();
                            return spawned;
                        }
                        entity.setLocationAndAngles(entityX, y, entityZ, world.rand.nextFloat() * 360.0F, 0.0F);
                        final Event.Result result = ForgeEventFactory.canEntitySpawn(entity, world, entityX, y, entityZ, null);
                        if (result == Event.Result.ALLOW || (result == Event.Result.DEFAULT
                            && entity.getCanSpawnHere() && entity.isNotColliding())) {
                            if (!ForgeEventFactory.doSpecialSpawn(entity, world, entityX, y, entityZ, null)) {
                                groupData = entity.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entity)), groupData);
                            }
                            if (entity.isNotColliding()) {
                                packSpawned++;
                                world.spawnEntity(entity);
                            } else {
                                entity.setDead();
                            }
                            if (packSpawned >= ForgeEventFactory.getMaxSpawnPackSize(entity)) {
                                continue chunkLoop;
                            }
                        }

                        // Preserve vanilla's accumulated return value, including its pack-size early exit.
                        spawned += packSpawned;
                    }
                }
            }
        }
        return spawned;
    }
}
