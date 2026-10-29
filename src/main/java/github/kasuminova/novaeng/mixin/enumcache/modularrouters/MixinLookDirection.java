package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.logic.compiled.CompiledActivatorModule.LookDirection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LookDirection.class, remap = false)
public abstract class MixinLookDirection {

    @Shadow
    @Final
    private static LookDirection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static LookDirection[] values() {
        return $VALUES;
    }
}
