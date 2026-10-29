package github.kasuminova.novaeng.mixin.enumcache.random_complement;

import com.circulation.random_complement.client.buttonsetting.Action;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Action.class, remap = false)
public abstract class MixinAction {

    @Shadow
    @Final
    private static Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Action[] values() {
        return $VALUES;
    }
}
