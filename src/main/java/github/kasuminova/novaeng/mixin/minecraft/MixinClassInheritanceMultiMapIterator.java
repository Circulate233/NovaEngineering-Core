package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.util.ClassFilteredIterable;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newIdentityHashSet()Ljava/util/Set;", remap = false))
    public Set<?> nova$redSet() {
        return new ReferenceOpenHashSet<>();
    }

    /**
     * @author NovaEngineering contributors
     * @reason Filter the stored class list without allocating Guava filtering wrappers.
     */
    @Overwrite
    public <S> Iterable<S> getByClass(final Class<S> clazz) {
        final Class<?> key = this.initializeClassLookup(clazz);
        List<T> list = this.map.get(key);
        if (list == null) {
            return Collections.emptyList();
        }
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        return new ClassFilteredIterable<>(list, clazz);
    }
}
