package github.kasuminova.novaeng.client.ctm;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.mixin.ctm.AccessorModelCTMReplay;
import github.kasuminova.novaeng.mixin.ctm.InvokerModelCTMReplay;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import team.chisel.ctm.client.model.ModelCTM;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Concurrent generation-scoped implementation of {@link CtmBakeTraceIndex}.
 *
 * <p>One owner performs each real bake while peers await its future. Published traces contain only
 * calls to ModelCTM's private texture lambda, so replay creates fresh CTM texture objects rather
 * than sharing mutable texture state between wrappers.</p>
 */
public final class CtmBakeTraceIndexImpl implements CtmBakeTraceIndex {

    private static final String VANILLA = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper";
    private static final String MULTIPART = "net.minecraftforge.client.model.ModelLoader$MultipartModel";
    private static final String WEIGHTED = "net.minecraftforge.client.model.ModelLoader$WeightedRandomModel";
    private static final CtmBakeTraceIndex INSTANCE = new CtmBakeTraceIndexImpl();

    private final AtomicLong nextGeneration = new AtomicLong();
    private final AtomicReference<Generation> activeGeneration = new AtomicReference<>();
    private final ThreadLocal<ArrayDeque<Recorder>> recorders = new ThreadLocal<>();

    private CtmBakeTraceIndexImpl() {
    }

    /** Returns the shared trace owner used by CTM mixins. */
    public static CtmBakeTraceIndex instance() {
        return INSTANCE;
    }

    @Override
    public void begin() {
        final Generation generation = new Generation(this.nextGeneration.incrementAndGet());
        if (!this.activeGeneration.compareAndSet(null, generation)) {
            final IllegalStateException failure = new IllegalStateException("A CTM bake generation is already active");
            NovaEngineeringCore.log.error("Failed to begin a nested CTM bake generation.", failure);
            throw failure;
        }
    }

    @Override
    public boolean isActive() {
        return this.activeGeneration.get() != null;
    }

    @Override
    public IBakedModel bakeOrReplay(final ModelCTM wrapper,
                                    final IModel sourceModel,
                                    final IModelState state,
                                    final VertexFormat format,
                                    final Function<ResourceLocation, TextureAtlasSprite> textureGetter,
                                    final IBakedModel existingParent,
                                    final BakeOperation original) {
        final Generation generation = this.activeGeneration.get();
        if (generation == null || !isSimpleWrapper(wrapper, sourceModel) || !isEligible(sourceModel)) {
            return original.call();
        }

        final Set<ResourceLocation> dependencyFingerprint = Set.copyOf(
            ((AccessorModelCTMReplay) wrapper).nova$getTextureDependencies());
        final TraceKey key = new TraceKey(
            sourceModel, state, format, textureGetter, dependencyFingerprint, generation.id);
        final TraceEntry created = new TraceEntry(Thread.currentThread());
        final TraceEntry existing = generation.traces.putIfAbsent(key, created);
        if (existing == null) {
            final Recorder recorder = this.pushRecorder(wrapper);
            try {
                original.call();
                final Trace trace = new Trace(List.copyOf(recorder.locations));
                created.future.complete(trace);
                return existingParent;
            } catch (final Throwable failure) {
                created.future.completeExceptionally(failure);
                generation.traces.remove(key, created);
                NovaEngineeringCore.log.error("Failed to capture CTM bake trace for {}.",
                    sourceModel.getClass().getName(), failure);
                throw propagate(failure);
            } finally {
                this.popRecorder();
            }
        }

        if (existing.owner == Thread.currentThread()) {
            return original.call();
        }
        final Trace trace = await(existing.future);
        if (this.activeGeneration.get() != generation) {
            generation.traces.remove(key, existing);
            return original.call();
        }

        prepareReplay(wrapper);
        final InvokerModelCTMReplay invoker = (InvokerModelCTMReplay) wrapper;
        for (final ResourceLocation location : trace.locations) {
            invoker.nova$replayTextureAccess(textureGetter, location);
        }
        return existingParent;
    }

    @Override
    public void recordTextureAccess(final ModelCTM wrapper, final ResourceLocation location) {
        final ArrayDeque<Recorder> stack = this.recorders.get();
        if (stack != null) {
            final Recorder recorder = stack.peek();
            if (recorder != null && recorder.wrapper == wrapper) {
                recorder.locations.add(location);
            }
        }
    }

    @Override
    public void end() {
        final Generation generation = this.activeGeneration.getAndSet(null);
        if (generation == null) {
            final IllegalStateException failure = new IllegalStateException("No CTM bake generation is active");
            NovaEngineeringCore.log.error("Failed to end a CTM bake generation.", failure);
            throw failure;
        }
        generation.traces.clear();
    }

    private Recorder pushRecorder(final ModelCTM wrapper) {
        ArrayDeque<Recorder> stack = this.recorders.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            this.recorders.set(stack);
        }
        final Recorder recorder = new Recorder(wrapper);
        stack.push(recorder);
        return recorder;
    }

    private void popRecorder() {
        final ArrayDeque<Recorder> stack = this.recorders.get();
        stack.pop();
        if (stack.isEmpty()) {
            this.recorders.remove();
        }
    }

    private static boolean isSimpleWrapper(final ModelCTM wrapper, final IModel sourceModel) {
        final AccessorModelCTMReplay access = (AccessorModelCTMReplay) wrapper;
        return access.nova$getModelInfo() == null
            && access.nova$getOverrides().isEmpty()
            && access.nova$getMetaOverrides().isEmpty()
            && access.nova$getTextures().isEmpty()
            && access.nova$getLayers() == 0
            && access.nova$getUvLock() == null
            && access.nova$getVanillaModel() == sourceModel
            && (access.nova$getSpriteOverrides() == null || access.nova$getSpriteOverrides().isEmpty())
            && (access.nova$getTextureOverrides() == null || access.nova$getTextureOverrides().isEmpty());
    }

    private static boolean isEligible(final IModel root) {
        final Set<IModel> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return isEligible(root, visited);
    }

    private static boolean isEligible(final IModel model, final Set<IModel> visited) {
        if (!visited.add(model)) {
            return true;
        }
        final String className = model.getClass().getName();
        if (!className.equals(VANILLA) && !className.equals(MULTIPART) && !className.equals(WEIGHTED)) {
            return false;
        }
        if (!(model instanceof CtmReplayModel replayModel)) {
            return false;
        }
        for (final IModel child : replayModel.nova$getCtmReplayChildren()) {
            if (child == null || !isEligible(child, visited)) {
                return false;
            }
        }
        return true;
    }

    private static void prepareReplay(final ModelCTM wrapper) {
        final AccessorModelCTMReplay access = (AccessorModelCTMReplay) wrapper;
        if (access.nova$getSpriteOverrides() == null) {
            access.nova$setSpriteOverrides(new Int2ObjectOpenHashMap<>());
        }
        if (access.nova$getTextureOverrides() == null) {
            access.nova$setTextureOverrides(new ConcurrentHashMap<>());
        }
    }

    private static Trace await(final CompletableFuture<Trace> future) {
        try {
            return future.join();
        } catch (final CompletionException failure) {
            throw propagate(failure.getCause() == null ? failure : failure.getCause());
        }
    }

    private static RuntimeException propagate(final Throwable failure) {
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("Unexpected checked CTM bake failure", failure);
    }

    private static final class Generation {
        private final long id;
        private final ConcurrentHashMap<TraceKey, TraceEntry> traces = new ConcurrentHashMap<>();

        private Generation(final long id) {
            this.id = id;
        }
    }

    private static final class TraceEntry {
        private final Thread owner;
        private final CompletableFuture<Trace> future = new CompletableFuture<>();

        private TraceEntry(final Thread owner) {
            this.owner = owner;
        }
    }

    private static final class Recorder {
        private final ModelCTM wrapper;
        private final List<ResourceLocation> locations = new ArrayList<>();

        private Recorder(final ModelCTM wrapper) {
            this.wrapper = wrapper;
        }
    }

    private record Trace(List<ResourceLocation> locations) {
    }

    private record TraceKey(IModel model, IModelState state, VertexFormat format,
                            Function<ResourceLocation, TextureAtlasSprite> textureGetter,
                            Set<ResourceLocation> dependencyFingerprint, long generation) {

        @Override
            public boolean equals(final Object other) {
                if (this == other) {
                    return true;
                }
                if (!(other instanceof TraceKey(
                    IModel model1, IModelState state1, VertexFormat format1,
                    Function<ResourceLocation, TextureAtlasSprite> getter, Set<ResourceLocation> fingerprint,
                    long generation1
                ))) {
                    return false;
                }
                return this.model == model1 && this.state == state1 && this.format == format1
                    && this.textureGetter == getter && this.generation == generation1
                    && this.dependencyFingerprint.equals(fingerprint);
            }

            @Override
            public int hashCode() {
                int result = System.identityHashCode(this.model);
                result = 31 * result + System.identityHashCode(this.state);
                result = 31 * result + System.identityHashCode(this.format);
                result = 31 * result + System.identityHashCode(this.textureGetter);
                result = 31 * result + this.dependencyFingerprint.hashCode();
                result = 31 * result + Long.hashCode(this.generation);
                return result;
            }
        }
}
