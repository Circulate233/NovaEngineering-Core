package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.resource.SelectiveReloadStateHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SelectiveReloadStateHandler.class, remap = false)
public abstract class MixinSelectiveReloadStateHandler {

    @Shadow
    @Final
    private static SelectiveReloadStateHandler[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SelectiveReloadStateHandler[] values() {
        return $VALUES;
    }
}
