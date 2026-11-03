package github.kasuminova.novaeng.mixin.librarianlib;

import com.google.common.collect.HashBiMap;
import com.teamwizardry.librarianlib.features.saving.serializers.builtin.core.NamedDynamicRegistryManager.NamedDynamicRegistry;

import github.kasuminova.novaeng.common.util.NamedDynamicIdAccess;
import github.kasuminova.novaeng.common.util.collection.Reference2IntBiMap;
import github.kasuminova.novaeng.common.util.collection.Reference2IntHashBiMap;

import kotlin.jvm.internal.Intrinsics;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = NamedDynamicRegistry.class, remap = false)
public abstract class MixinNamedDynamicRegistryPrimitiveBiMap implements NamedDynamicIdAccess {
    @Unique
    private final Reference2IntBiMap<Class<?>> nova$ids = new Reference2IntHashBiMap<>();
    @Shadow
    @Final
    private HashBiMap<Class<?>, String> map;
    @Shadow
    private int nextID;

    @Override
    public int nova$getNamedDynamicId(final Class<?> type) {
        return nova$ids.getIntOrDefault(type, -1);
    }

    /**
     * @author NovaEngineering
     * @reason Keep names unchanged and store numeric IDs primitively.
     */
    @Overwrite
    public void add(final Class<?> clazz, final String name) {
        Intrinsics.checkNotNullParameter(clazz, "clazz");
        Intrinsics.checkNotNullParameter(name, "name");
        if (map.containsKey(clazz))
            throw new IllegalArgumentException("Duplicate type registration for `" + clazz + "`!");
        map.put(clazz, name);
        nova$ids.putDiscard(clazz, nextID++, false);
    }

    /**
     * @author NovaEngineering
     * @reason Preserve the nullable public ID result.
     */
    @Overwrite
    public Integer getID(final Class<?> clazz) {
        Intrinsics.checkNotNullParameter(clazz, "clazz");
        return nova$ids.get(clazz);
    }

    /**
     * @author NovaEngineering
     * @reason Read a primitive registry ID.
     */
    @Overwrite
    public Class<?> getByID(final int id) {
        return nova$ids.inverse().get(id);
    }

    /**
     * @author NovaEngineering
     * @reason Test a primitive registry ID.
     */
    @Overwrite
    public boolean contains(final int id) {
        return nova$ids.containsValue(id);
    }
}
