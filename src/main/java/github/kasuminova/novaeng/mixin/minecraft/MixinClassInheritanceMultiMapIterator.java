package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.util.ClassFilteredIterable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.util.ClassInheritanceMultiMap;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Answers {@code getByClass} with a walk that allocates one object instead of three.
 *
 * <p>The original returns an {@code Iterable} whose {@code iterator()} takes an iterator from the section's list and
 * wraps it in {@code Iterators.filter} - an anonymous {@code Iterable}, an {@code ArrayList$Itr} and a filtering
 * iterator, all discarded as soon as the loop ends. This is not an idle path: it is what
 * {@code World.getEntitiesWithinAABB} goes through for every loaded chunk section it touches, on both the client and
 * the server, and by the time the world is up it is the largest single allocation site on the server thread - around
 * 67 MB/s of the thread's 83, four fifths of everything the server thread allocates.</p>
 *
 * <p>The replacement walks the backing list by index and applies the same {@code isInstance} test the filter used, so
 * the elements handed out are the same ones in the same order. The returned iterable is itself the iterator, which
 * every vanilla caller is happy with because each of them iterates the result exactly once; a second
 * {@code iterator()} call - which no vanilla caller makes, but which costs nothing to keep working - gets a fresh
 * walk of the same list.</p>
 *
 * <p>Removal is not supported, which is not a change: {@code Iterators.filter} answered with an
 * {@code AbstractIterator} that did not support it either.</p>
 *
 * <p>{@code getByClass} is an overwrite and so has no runtime fallback; the mixin is instead gated at apply time by
 * {@code MixinDecisions}, the way the other overwrites in this codebase are.</p>
 */
@Mixin(value = ClassInheritanceMultiMap.class)
public abstract class MixinClassInheritanceMultiMapIterator<T> {

    @Mutable
    @Shadow
    @Final
    private Map<Class<?>, List<T>> map;

    @Shadow
    protected abstract Class<?> initializeClassLookup(Class<?> clazz);

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/util/ClassInheritanceMultiMap;map:Ljava/util/Map;", opcode = Opcodes.PUTFIELD))
    public void nova$redMap(ClassInheritanceMultiMap<?> instance, Map<Class<?>, List<T>> value) {
        map = new Reference2ObjectOpenHashMap<>();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newIdentityHashSet()Ljava/util/Set;"))
    public Set<?> nova$redSet() {
        return new ReferenceOpenHashSet<>();
    }

    @Overwrite
    public <S> Iterable<S> getByClass(final Class<S> clazz) {
        final Class<?> key = this.initializeClassLookup(clazz);
        List<T> list = this.map.get(key);
        if (list == null) {
            list = new ObjectArrayList<>(0);
            this.map.put(key, list);
        }
        return new ClassFilteredIterable<>(list, clazz);
    }
}
