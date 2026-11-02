package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.model.ModelParrot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModelParrot.State.class, remap = false)
public abstract class MixinModelParrotState {

    @Shadow
    @Final
    private static ModelParrot.State[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModelParrot.State[] values() {
        return $VALUES;
    }
}
