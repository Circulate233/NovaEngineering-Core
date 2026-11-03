package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.Iterator;

@Mixin(Chunk.class)
public abstract class MixinChunkEntityQuery {

    @Redirect(
        method = "getEntitiesWithinAABBForEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Lcom/google/common/base/Predicate;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ClassInheritanceMultiMap;isEmpty()Z", remap = false),
        require = 1
    )
    private boolean nova$emptyWhenSectionMissing(final ClassInheritanceMultiMap<Entity> entities) {
        return entities == null || entities.isEmpty();
    }

    @Redirect(
        method = "getEntitiesWithinAABBForEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Lcom/google/common/base/Predicate;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ClassInheritanceMultiMap;iterator()Ljava/util/Iterator;"),
        require = 1
    )
    private Iterator<Entity> nova$iterateValues(final ClassInheritanceMultiMap<Entity> entities) {
        return entities == null ? Collections.emptyIterator()
            : ((AccessorClassInheritanceMultiMap<Entity>) entities).nova$getValues().iterator();
    }
}
