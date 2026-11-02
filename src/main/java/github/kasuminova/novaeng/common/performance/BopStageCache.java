package github.kasuminova.novaeng.common.performance;

import biomesoplenty.api.generation.BOPGeneratorBase;
import biomesoplenty.api.generation.GeneratorStage;
import biomesoplenty.api.generation.IGenerator;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class BopStageCache {
    private static final AtomicLong REVISION = new AtomicLong();
    private static final ClassValue<Boolean> STANDARD = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            try {
                final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                return lookup.revealDirect(lookup.findVirtual(type, "getStage", MethodType.methodType(GeneratorStage.class)))
                             .getDeclaringClass() == BOPGeneratorBase.class
                    && lookup.revealDirect(lookup.findVirtual(type, "setStage", MethodType.methodType(void.class, GeneratorStage.class)))
                             .getDeclaringClass() == BOPGeneratorBase.class;
            } catch (final ReflectiveOperationException unavailable) {
                return false;
            }
        }
    };
    private final Reference2ObjectOpenHashMap<GeneratorStage, ImmutableCollection<IGenerator>> stages = new Reference2ObjectOpenHashMap<>();
    private long revision = -1;
    private boolean checked;
    private boolean cacheable;

    public static void stageChanged() {
        REVISION.incrementAndGet();
    }

    public void invalidate() {
        stages.clear();
        checked = false;
    }

    public ImmutableCollection<IGenerator> get(final Map<String, IGenerator> generators, final GeneratorStage stage) {
        final long current = REVISION.get();
        if (current != revision) {
            invalidate();
            revision = current;
        }
        if (!checked) {
            cacheable = true;
            for (final IGenerator generator : generators.values()) {
                if (generator == null || !STANDARD.get(generator.getClass())) {
                    cacheable = false;
                    break;
                }
            }
            checked = true;
        }
        final ImmutableCollection<IGenerator> cached = stages.get(stage);
        if (cached != null && cacheable) return cached;
        final ObjectArrayList<IGenerator> selected = new ObjectArrayList<>();
        for (final IGenerator generator : generators.values()) {
            if (generator.getStage() == stage) selected.add(generator);
        }
        final ImmutableCollection<IGenerator> result = ImmutableList.copyOf(selected);
        if (cacheable) stages.put(stage, result);
        return result;
    }
}
