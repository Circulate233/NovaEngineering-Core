package github.kasuminova.novaeng.mixin.minecraft;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Iterates the known structure starts from a cached array instead of probing the backing open
 * hash map on every query.
 *
 * <p>{@code getStructureAt} and {@code isPositionInStructure} walk every structure start the
 * generator knows about, and entity spawning asks for {@code getPossibleCreatures} on every spawn
 * attempt, so this walk happens constantly during normal play. Walking an open hash map costs a
 * table probe per slot, including the unused ones; the array holds exactly the same elements in the
 * same order, so results are unchanged.</p>
 *
 * <p>Only the set of starts is cached, never their bounds: components are still read from the live
 * start objects, so a structure that grows while it is being generated stays visible exactly as
 * before. The cache is rebuilt whenever the map's size changes, which covers every insertion, as
 * structures are never removed.</p>
 */
@Mixin(MapGenStructure.class)
public abstract class MixinMapGenStructure {

    @Shadow
    protected Long2ObjectMap<StructureStart> structureMap;

    @Unique
    private volatile StructureStart[] nova$starts;

    @Shadow
    protected abstract void initializeStructureData(World worldIn);

    @Unique
    private StructureStart[] nova$starts() {
        StructureStart[] cache = this.nova$starts;
        if (cache == null || cache.length != this.structureMap.size()) {
            cache = this.structureMap.values().toArray(new StructureStart[0]);
            this.nova$starts = cache;
        }
        return cache;
    }

    /**
     * @author circulation
     * @reason Walk the cached array instead of the backing map.
     */
    @Overwrite
    protected StructureStart getStructureAt(final BlockPos pos) {
        for (final StructureStart start : nova$starts()) {
            if (start.isSizeableStructure() && start.getBoundingBox().isVecInside(pos)) {
                for (final StructureComponent component : start.getComponents()) {
                    if (component.getBoundingBox().isVecInside(pos)) {
                        return start;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @author circulation
     * @reason Walk the cached array instead of the backing map.
     */
    @Overwrite
    public boolean isPositionInStructure(final World worldIn, final BlockPos pos) {
        this.initializeStructureData(worldIn);
        for (final StructureStart start : nova$starts()) {
            if (start.isSizeableStructure() && start.getBoundingBox().isVecInside(pos)) {
                return true;
            }
        }
        return false;
    }
}
