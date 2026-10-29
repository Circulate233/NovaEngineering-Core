package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.projectile.EntityFishHook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityFishHook.State.class, remap = false)
public abstract class MixinEntityFishHookState {

    @Shadow
    @Final
    private static EntityFishHook.State[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityFishHook.State[] values() {
        return $VALUES;
    }
}
