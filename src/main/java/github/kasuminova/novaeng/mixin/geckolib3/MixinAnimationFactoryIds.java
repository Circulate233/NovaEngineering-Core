package github.kasuminova.novaeng.mixin.geckolib3;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.HashMap;

@Mixin(value = AnimationFactory.class, remap = false)
public abstract class MixinAnimationFactoryIds {
    // NEW redirects must return a real instance; the legacy private map is never used by the replacement method.
    @Unique
    private static final HashMap<?, ?> nova$unusedLegacyMap = new HashMap<>(0);
    @Unique
    private final Int2ObjectMap<AnimationData> nova$animationData = new Int2ObjectOpenHashMap<>(2);
    @Shadow
    @Final
    private IAnimatable animatable;
    @Unique
    private AnimationData nova$nullIdData;

    // getOrCreateAnimationData is the only consumer of the original private map.
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "()Ljava/util/HashMap;"), require = 1, allow = 1)
    private HashMap<?, ?> nova$omitBoxedAnimationData() {
        return nova$unusedLegacyMap;
    }

    /**
     * @author NovaEngineering
     * @reason Store animation IDs as primitive ints, preserving nullable IDs and registration-before-publication.
     */
    @Overwrite
    public AnimationData getOrCreateAnimationData(final Integer uniqueID) {
        if (uniqueID == null) {
            if (nova$nullIdData == null) {
                final AnimationData data = new AnimationData();
                animatable.registerControllers(data);
                nova$nullIdData = data;
            }
            return nova$nullIdData;
        }
        final int id = uniqueID;
        AnimationData data = nova$animationData.get(id);
        if (data == null) {
            data = new AnimationData();
            animatable.registerControllers(data);
            nova$animationData.put(id, data);
        }
        return data;
    }
}
