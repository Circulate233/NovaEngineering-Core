package github.kasuminova.novaeng.mixin.enumcache.journeymap;

import journeymap.client.render.draw.DrawStep.Pass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Pass.class, remap = false)
public abstract class MixinPass {

    @Shadow
    @Final
    private static Pass[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Pass[] values() {
        return $VALUES;
    }
}
