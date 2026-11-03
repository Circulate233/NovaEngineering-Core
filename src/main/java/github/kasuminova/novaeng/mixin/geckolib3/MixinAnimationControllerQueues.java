package github.kasuminova.novaeng.mixin.geckolib3;

import github.kasuminova.novaeng.client.animation.AnimationPointSampling;
import github.kasuminova.novaeng.client.animation.AnimationQueueReuse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.keyframe.AnimationPoint;
import software.bernie.geckolib3.core.keyframe.BoneAnimationQueue;
import software.bernie.geckolib3.core.keyframe.KeyFrame;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.core.util.Axis;
import software.bernie.shadowed.eliotlash.mclib.math.IValue;

import java.util.HashMap;
import java.util.List;

@Mixin(value = AnimationController.class, remap = false)
public abstract class MixinAnimationControllerQueues {
    @Shadow
    @Final
    private HashMap<String, BoneAnimationQueue> boneAnimationQueues;

    /**
     * @author NovaEngineering
     * @reason Reuse empty axis queues while the controller still targets the same bones.
     */
    @Overwrite
    private void createInitialQueues(final List<IBone> bones) {
        AnimationQueueReuse.reset(boneAnimationQueues, bones);
    }

    /**
     * @author NovaEngineering
     * @reason Compute the local keyframe time without allocating a KeyFrameLocation intermediary.
     */
    @Overwrite
    private AnimationPoint getAnimationPointAtTick(final List<KeyFrame<IValue>> frames, final double tick,
                                                   final boolean rotation, final Axis axis) {
        return AnimationPointSampling.sample(frames, tick, rotation, axis);
    }
}
