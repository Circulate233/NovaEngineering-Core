package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.obj.OBJModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = OBJModel.OBJState.Operation.class, remap = false)
public abstract class MixinOBJStateOperation {

    @Shadow
    @Final
    private static OBJModel.OBJState.Operation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static OBJModel.OBJState.Operation[] values() {
        return $VALUES;
    }
}
