package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Stops a spawn attempt from resolving the same position's spawn list twice.
 *
 * <p>An attempt first picks a candidate through {@code getSpawnListEntryForTypeAt} and then asks
 * {@code canCreatureTypeSpawnHere} whether that candidate may spawn there. Both methods do the same work: read the
 * biome's spawn list for the position, hand it to the {@code PotentialSpawns} event, and then either pick a weighted
 * entry from the result or test the candidate against it. Between the two calls the shared mutable position is not
 * touched and the world state does not change, so the second call rebuilds the very list the first one already had.</p>
 *
 * <p>The list resolved for a position is kept for the accompanying check and dropped when a different position or
 * creature type comes along, so a handler that derives its answer from world state still gets asked again for every
 * new position. Nothing about the decision is cached: the candidate is still chosen by the vanilla weighted pick, and
 * the check still decides the outcome from that list.</p>
 */
@Mixin(WorldEntitySpawner.class)
public abstract class MixinWorldEntitySpawner {

    /**
     * Packed position the held list belongs to, or {@link Long#MIN_VALUE} when no list is held.
     *
     * <p>Long.MIN_VALUE cannot be produced by {@link BlockPos#toLong()}, whose coordinates are within a few million,
     * so it works as the "empty" marker without a separate flag.</p>
     */
    @Unique
    private long nova$listPos = Long.MIN_VALUE;

    /** Creature type the held list was resolved for. */
    @Unique
    private EnumCreatureType nova$listType;

    /**
     * The list handed to the {@code PotentialSpawns} event for the position above, which is the list the candidate is
     * picked from.
     */
    @Unique
    private List<Biome.SpawnListEntry> nova$list;

    /**
     * Resolves the spawn list once per position so the accompanying check can reuse it.
     *
     * @param world the spawning world
     * @param type the creature type being spawned
     * @param pos the position being tested
     * @return the weighted candidate for the position, or {@code null} when it has no spawn list
     */
    @Redirect(
        method = "findChunksForSpawning",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldServer;getSpawnListEntryForTypeAt("
                + "Lnet/minecraft/entity/EnumCreatureType;Lnet/minecraft/util/math/BlockPos;)"
                + "Lnet/minecraft/world/biome/Biome$SpawnListEntry;"),
        require = 1
    )
    private Biome.SpawnListEntry nova$resolveOnce(final WorldServer world, final EnumCreatureType type,
                                                  final BlockPos pos) {
        final List<Biome.SpawnListEntry> list = net.minecraftforge.event.ForgeEventFactory.getPotentialSpawns(
            world, type, pos, world.getChunkProvider().getPossibleCreatures(type, pos));
        this.nova$list = list;
        this.nova$listPos = pos.toLong();
        this.nova$listType = type;

        if (list == null || list.isEmpty()) {
            return null;
        }
        return net.minecraft.util.WeightedRandom.getRandomItem(world.rand, list);
    }

    /**
     * Answers the membership check from the list the candidate was picked from.
     *
     * <p>Vanilla rebuilds the list and asks whether it contains the candidate. The candidate came out of that list, so
     * the answer is yes - and the empty case is unreachable, because the caller breaks out of the attempt as soon as
     * the candidate is {@code null}. When the position or type is not the one the list was resolved for, the world's
     * own answer is used instead, so the method stays correct whatever the caller does.</p>
     *
     * @param world the spawning world
     * @param type the creature type being spawned
     * @param entry the candidate previously returned for this position
     * @param pos the position being tested
     * @return whether the candidate may spawn at the position
     */
    @Redirect(
        method = "findChunksForSpawning",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldServer;canCreatureTypeSpawnHere("
                + "Lnet/minecraft/entity/EnumCreatureType;Lnet/minecraft/world/biome/Biome$SpawnListEntry;"
                + "Lnet/minecraft/util/math/BlockPos;)Z"),
        require = 1
    )
    private boolean nova$checkAgainstHeldList(final WorldServer world, final EnumCreatureType type,
                                              final Biome.SpawnListEntry entry, final BlockPos pos) {
        final List<Biome.SpawnListEntry> list = this.nova$list;
        if (list != null && this.nova$listType == type && this.nova$listPos == pos.toLong()) {
            return list.contains(entry);
        }
        return world.canCreatureTypeSpawnHere(type, entry, pos);
    }
}
