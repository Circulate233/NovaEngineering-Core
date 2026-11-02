package github.kasuminova.novaeng.mixin.minecraft;

import com.google.common.collect.ImmutableList;
import github.kasuminova.novaeng.common.util.LazyEntityLists;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Vanilla fills {@code entityLists} with 16 eagerly created ClassInheritanceMultiMap
 * instances in the chunk constructor, each carrying its own map, key set and value list.
 * A chunk that never holds an entity still pays 64 allocations and ~2.7 KB, most of a
 * plain chunk's footprint.
 * <p>
 * Slots stay null until first use: read paths treat a null slot as empty, the write path
 * creates only the section it needs, and {@code getEntityLists()} still returns a fully
 * populated array so external callers keep vanilla behaviour.
 */
@Mixin(Chunk.class)
public abstract class MixinChunkLazyEntityLists {

    @Shadow
    @Final
    private ClassInheritanceMultiMap<Entity>[] entityLists;

    /**
     * Hands the constructor a zero-length array so the loop that would instantiate all 16
     * sections never runs; the real array keeps its nulls.
     */
    @Redirect(
        method = "<init>(Lnet/minecraft/world/World;II)V",
        at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/chunk/Chunk;entityLists:[Lnet/minecraft/util/ClassInheritanceMultiMap;",
            opcode = 0xb4,
            ordinal = 0)
    )
    private ClassInheritanceMultiMap<Entity>[] nova$skipEagerSections(final Chunk instance) {
        return LazyEntityLists.EMPTY;
    }

    /**
     * @author circulation
     * @reason Create only the section the entity lands in, instead of all 16 up front.
     */
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void nova$ensureSection(final Entity entityIn, final CallbackInfo ci) {
        final int section = MathHelper.clamp(
            MathHelper.floor(entityIn.posY / 16.0D), 0, this.entityLists.length - 1);
        if (this.entityLists[section] == null) {
            this.entityLists[section] = LazyEntityLists.create(Entity.class);
        }
    }

    /**
     * @author circulation
     * @reason Materialise every slot before the array escapes to callers that expect a
     * fully populated array.
     */
    @Inject(method = "getEntityLists", at = @At("HEAD"))
    private void nova$fillBeforeExport(final CallbackInfoReturnable<ClassInheritanceMultiMap<Entity>[]> cir) {
        LazyEntityLists.fill(this.entityLists);
    }

    /**
     * Keep vanilla's load lifecycle intact while treating an unallocated section as empty.
     */
    @Redirect(
        method = "onLoad()V",
        at = @At(value = "INVOKE",
            target = "Lcom/google/common/collect/ImmutableList;copyOf(Ljava/util/Collection;)Lcom/google/common/collect/ImmutableList;",
            remap = false),
        require = 1)
    private ImmutableList<Entity> nova$copyLoadedEntities(final Collection<Entity> entities) {
        return entities == null ? ImmutableList.of() : ImmutableList.copyOf(entities);
    }

    /**
     * Forge's player-respawn pass must skip sections that have never held entities.
     */
    @Redirect(
        method = "onUnload()V",
        at = @At(value = "INVOKE",
            target = "Ljava/util/Arrays;stream([Ljava/lang/Object;)Ljava/util/stream/Stream;",
            remap = false),
        require = 1)
    @SuppressWarnings("unchecked")
    private Stream<ClassInheritanceMultiMap<Entity>> nova$streamPresentEntitySections(
        final Object[] sections) {
        final ClassInheritanceMultiMap<Entity>[] typedSections =
            (ClassInheritanceMultiMap<Entity>[]) sections;
        return Arrays.stream(typedSections).filter(Objects::nonNull);
    }

    /**
     * Keep vanilla's unload lifecycle intact while skipping empty lazy slots.
     */
    @Redirect(
        method = "onUnload()V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;unloadEntities(Ljava/util/Collection;)V"),
        require = 1)
    private void nova$unloadPresentEntities(final World world, final Collection<Entity> entities) {
        if (entities != null) {
            world.unloadEntities(entities);
        }
    }

    /**
     * A null slot means the section never held an entity, so it reports no matches.
     */
    @Redirect(
        method = "getEntitiesOfTypeWithinAABB",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/ClassInheritanceMultiMap;getByClass(Ljava/lang/Class;)Ljava/lang/Iterable;")
    )
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Iterable<Entity> nova$nullSafeByClass(final ClassInheritanceMultiMap<Entity> section,
                                                  final Class<?> clazz) {
        if (section == null) {
            return Collections.emptyList();
        }
        return section.getByClass((Class) clazz);
    }

    /**
     * A null slot has nothing to remove.
     */
    @Redirect(
        method = "removeEntityAtIndex",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/ClassInheritanceMultiMap;remove(Ljava/lang/Object;)Z")
    )
    private boolean nova$nullSafeRemove(final ClassInheritanceMultiMap<Entity> section, final Object entity) {
        return section != null && section.remove(entity);
    }
}
