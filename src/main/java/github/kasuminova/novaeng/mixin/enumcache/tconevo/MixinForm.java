package github.kasuminova.novaeng.mixin.enumcache.tconevo;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xyz.phanta.tconevo.item.ItemMetal.Form;

@Mixin(value = Form.class, remap = false)
public abstract class MixinForm {

    @Shadow
    @Final
    private static Form[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Form[] values() {
        return $VALUES;
    }
}
