package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.mmce.common.util.DynamicPattern;
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.modifier.MultiBlockModifierReplacement;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

@Mixin(value = BlockArrayCache.class, remap = false)
public abstract class MixinBlockArrayCache {

    @Shadow
    private static void buildBlockArrayCache(BlockArray blockArray) {
    }

    @Shadow
    private static void buildMultiBlockModifierCache(List<MultiBlockModifierReplacement> replacementList) {
    }

    @Shadow
    private static void buildDynamicPatternCache(TaggedPositionBlockArray blockArray) {
    }

    @Inject(method = "buildCache", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER))
    private static void buildCache(Collection<DynamicMachine> machines, CallbackInfo ci) {
        NEWMachineAssemblyManager.Companion.getAllConstructors().parallelStream().forEach(machine -> {
            TaggedPositionBlockArray blockArray = machine.getPattern();
            buildBlockArrayCache(blockArray);
            buildMultiBlockModifierCache(machine.getMultiBlockModifiers());

            for (DynamicPattern pattern : machine.getDynamicPatterns().values()) {
                buildDynamicPatternCache(pattern.getPattern());
                TaggedPositionBlockArray patternEnd = pattern.getPatternEnd();
                if (patternEnd != null) {
                    buildDynamicPatternCache(patternEnd);
                }
            }

        });
    }

}