package github.kasuminova.novaeng.mixin.geckolib3;

import github.kasuminova.novaeng.client.animation.AnimationTrackerAccess;
import github.kasuminova.novaeng.client.animation.AnimationTrackerFrames;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib3.core.processor.AnimationProcessor;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.core.snapshot.DirtyTracker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(value = AnimationProcessor.class, remap = false)
public abstract class MixinAnimationProcessorTrackers implements AnimationTrackerAccess {
    // The redirected operations use the primitive set; NEW still requires a non-null legacy instance.
    @Unique
    private static final HashSet<?> nova$unusedLegacySet = new HashSet<>(0);
    @Unique
    private final AnimationTrackerFrames.Pool nova$trackerPool = new AnimationTrackerFrames.Pool();
    @Unique
    private final IntSet nova$animatedIds = new IntOpenHashSet(2);
    @Shadow
    private List<IBone> modelRendererList;
    @Unique
    private boolean nova$animatedNullId;

    // The original private set is unused after all three tickAnimation operations are redirected.
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "()Ljava/util/HashSet;"), require = 1, allow = 1)
    private HashSet<?> nova$omitBoxedAnimatedIds() {
        return nova$unusedLegacySet;
    }

    @Redirect(method = "tickAnimation", at = @At(value = "INVOKE", target = "Ljava/util/Set;clear()V"), require = 1, allow = 1)
    private void nova$clearAnimatedIds(final Set<?> ignored) {
        nova$animatedIds.clear();
        nova$animatedNullId = false;
    }

    @Redirect(method = "tickAnimation", at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"), require = 1, allow = 1)
    private boolean nova$containsAnimatedId(final Set<?> ignored, final Object value) {
        return value == null ? nova$animatedNullId : nova$animatedIds.contains(((Integer) value).intValue());
    }

    @Redirect(method = "tickAnimation", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"), require = 1, allow = 1)
    private boolean nova$addAnimatedId(final Set<?> ignored, final Object value) {
        if (value != null) return nova$animatedIds.add(((Integer) value).intValue());
        final boolean added = !nova$animatedNullId;
        nova$animatedNullId = true;
        return added;
    }

    @Override
    public AnimationTrackerFrames.Pool nova$trackerPool() {
        return nova$trackerPool;
    }

    /**
     * @author NovaEngineering
     * @reason Reuse dirty trackers only inside a scoped animation call; preserve independent fallback calls.
     */
    @Overwrite
    private HashMap<String, DirtyTracker> createNewDirtyTracker() {
        final var frame = AnimationTrackerFrames.current((AnimationProcessor<?>) (Object) this);
        return frame == null ? AnimationTrackerFrames.fresh(modelRendererList) : frame.prepare(modelRendererList);
    }

    /**
     * @author NovaEngineering
     * @reason Preserve first-match lookup without allocating a stream and predicate for each bone.
     */
    @Overwrite
    public IBone getBone(final String name) {
        for (int index = 0; index < modelRendererList.size(); index++) {
            final IBone bone = modelRendererList.get(index);
            if (bone.getName().equals(name)) return bone;
        }
        return null;
    }

    @Inject(method = "clearModelRendererList", at = @At("HEAD"), require = 1)
    private void nova$invalidateModelTrackers(final CallbackInfo ci) {
        nova$trackerPool.invalidate();
    }
}
