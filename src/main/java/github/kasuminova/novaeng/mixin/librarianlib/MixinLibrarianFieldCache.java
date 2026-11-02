package github.kasuminova.novaeng.mixin.librarianlib;

import com.teamwizardry.librarianlib.features.saving.FieldCache;
import com.teamwizardry.librarianlib.features.saving.FieldType;
import com.teamwizardry.librarianlib.features.saving.SavingFieldCache;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Mixin(value = SavingFieldCache.class, remap = false)
public abstract class MixinLibrarianFieldCache {
    // Completed tables need concurrent publication; fastutil's synchronized wrappers would lock every read.
    @Unique
    private static final ConcurrentMap<FieldType, Map<String, FieldCache>> nova$published = new ConcurrentHashMap<>();
    @Unique
    private static final ThreadLocal<Map<FieldType, Map<String, FieldCache>>> nova$building =
        ThreadLocal.withInitial(Object2ObjectOpenHashMap::new);
    @Shadow
    @Final
    private static LinkedHashMap<FieldType, Map<String, FieldCache>> atSaveMap;
    @Shadow
    @Final
    private static Set<String> alreadyDone;
    @Shadow
    @Final
    private static Set<String> methodGettersWOSetters;
    @Shadow
    @Final
    private static Set<String> methodSettersWOGetters;
    @Unique
    private static volatile boolean nova$mapExposed;

    /**
     * @author circulation
     * @reason Publish completed field tables once; serialize cold reflection work and support recursive first use.
     */
    @Overwrite
    public static @NotNull Map<String, FieldCache> getClassFields(final FieldType type) {
        Intrinsics.checkNotNullParameter(type, "type");
        if (!nova$mapExposed) {
            final Map<String, FieldCache> cached = nova$published.get(type);
            if (cached != null) {
                return cached;
            }
        }
        synchronized (SavingFieldCache.INSTANCE) {
            final Map<String, FieldCache> existing = atSaveMap.get(type);
            if (existing != null) {
                if (!nova$mapExposed) {
                    nova$published.put(type, existing);
                }
                return existing;
            }
            final Map<FieldType, Map<String, FieldCache>> building = nova$building.get();
            final Map<String, FieldCache> recursive = building.get(type);
            if (recursive != null) {
                return recursive;
            }
            final boolean nested = !building.isEmpty();
            final Set<String> savedDone = nested ? new ObjectOpenHashSet<>(alreadyDone) : null;
            final Set<String> savedGetters = nested ? new ObjectOpenHashSet<>(methodGettersWOSetters) : null;
            final Set<String> savedSetters = nested ? new ObjectOpenHashSet<>(methodSettersWOGetters) : null;
            alreadyDone.clear();
            methodGettersWOSetters.clear();
            methodSettersWOGetters.clear();
            final Map<String, FieldCache> fields = new Object2ObjectLinkedOpenHashMap<>();
            building.put(type, fields);
            try {
                SavingFieldCache.INSTANCE.buildClassFields(type, fields);
                SavingFieldCache.INSTANCE.buildClassGetSetters(type, fields);
                atSaveMap.put(type, fields);
                if (!nova$mapExposed) {
                    nova$published.put(type, fields);
                }
                return fields;
            } finally {
                building.remove(type);
                alreadyDone.clear();
                methodGettersWOSetters.clear();
                methodSettersWOGetters.clear();
                if (nested) {
                    alreadyDone.addAll(savedDone);
                    methodGettersWOSetters.addAll(savedGetters);
                    methodSettersWOGetters.addAll(savedSetters);
                }
            }
        }
    }

    /**
     * @author circulation
     * @reason Preserve the public mutable backing-map view; callers exposing it opt out of the private fast index.
     */
    @Overwrite
    public final @NotNull LinkedHashMap<FieldType, Map<String, FieldCache>> getAtSaveMap() {
        synchronized (SavingFieldCache.INSTANCE) {
            nova$mapExposed = true;
            nova$published.clear();
            return atSaveMap;
        }
    }
}
