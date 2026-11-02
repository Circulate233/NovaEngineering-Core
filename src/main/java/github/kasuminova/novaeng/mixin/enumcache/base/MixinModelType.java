package github.kasuminova.novaeng.mixin.enumcache.base;

import com.teamacronymcoders.base.client.models.generator.generatedmodel.ModelType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModelType.class, remap = false)
public abstract class MixinModelType {

    @Shadow
    @Final
    private static ModelType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModelType[] values() {
        return $VALUES;
    }
}
