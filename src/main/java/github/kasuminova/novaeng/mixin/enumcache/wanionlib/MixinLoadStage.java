package github.kasuminova.novaeng.mixin.enumcache.wanionlib;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import wanion.lib.module.LoadStage;

@Mixin(value = LoadStage.class, remap = false)
public abstract class MixinLoadStage {

    @Shadow
    @Final
    private static LoadStage[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static LoadStage[] values() {
        return $VALUES;
    }
}
