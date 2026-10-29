package github.kasuminova.novaeng.mixin.enumcache.draconicevolution;

import com.brandon3055.draconicevolution.blocks.tileentity.TileDraconiumChest.AutoSmeltMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AutoSmeltMode.class, remap = false)
public abstract class MixinAutoSmeltMode {

    @Shadow
    @Final
    private static AutoSmeltMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AutoSmeltMode[] values() {
        return $VALUES;
    }
}
