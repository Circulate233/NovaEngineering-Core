package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zone.rong.mixinbooter.Context;

@Mixin(value = Context.ModLoader.class, remap = false)
public abstract class MixinModLoader {

    @Shadow
    @Final
    private static Context.ModLoader[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Context.ModLoader[] values() {
        return $VALUES;
    }
}
