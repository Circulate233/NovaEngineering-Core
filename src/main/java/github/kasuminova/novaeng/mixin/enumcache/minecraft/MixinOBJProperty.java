package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.obj.OBJModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = OBJModel.OBJProperty.class, remap = false)
public abstract class MixinOBJProperty {

    @Shadow
    @Final
    private static OBJModel.OBJProperty[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static OBJModel.OBJProperty[] values() {
        return $VALUES;
    }
}
