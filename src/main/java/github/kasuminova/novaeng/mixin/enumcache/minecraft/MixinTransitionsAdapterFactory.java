package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.model.animation.AnimationStateMachine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AnimationStateMachine.TransitionsAdapterFactory.class, remap = false)
public abstract class MixinTransitionsAdapterFactory {

    @Shadow
    @Final
    private static AnimationStateMachine.TransitionsAdapterFactory[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AnimationStateMachine.TransitionsAdapterFactory[] values() {
        return $VALUES;
    }
}
