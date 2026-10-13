package github.kasuminova.novaeng.mixin.minecraft;

import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(Chunk.class)
public abstract class MixinChunkEntityQuery {

    @Shadow
    @Final
    private ClassInheritanceMultiMap<Entity>[] entityLists;

    @Overwrite
    public void getEntitiesWithinAABBForEntity(final Entity excluded,
                                               final AxisAlignedBB bounds,
                                               final List<Entity> result,
                                               final Predicate<? super Entity> predicate) {
        int minSection = MathHelper.floor((bounds.minY - World.MAX_ENTITY_RADIUS) / 16.0D);
        int maxSection = MathHelper.floor((bounds.maxY + World.MAX_ENTITY_RADIUS) / 16.0D);
        minSection = MathHelper.clamp(minSection, 0, this.entityLists.length - 1);
        maxSection = MathHelper.clamp(maxSection, 0, this.entityLists.length - 1);
        for (int section = minSection; section <= maxSection; section++) {
            final ClassInheritanceMultiMap<Entity> entities = this.entityLists[section];
            if (entities.isEmpty()) {
                continue;
            }
            final List<Entity> values = ((AccessorClassInheritanceMultiMap<Entity>) entities).nova$getValues();
            for (int index = 0, size = values.size(); index < size; index++) {
                final Entity entity = values.get(index);
                if (entity.getEntityBoundingBox().intersects(bounds) && entity != excluded
                    && (predicate == null || predicate.apply(entity))) {
                    result.add(entity);
                }
                final Entity[] parts = entity.getParts();
                if (parts == null) {
                    continue;
                }
                for (final Entity part : parts) {
                    if (part != excluded && part.getEntityBoundingBox().intersects(bounds)
                        && (predicate == null || predicate.apply(part))) {
                        result.add(part);
                    }
                }
            }
        }
    }
}
