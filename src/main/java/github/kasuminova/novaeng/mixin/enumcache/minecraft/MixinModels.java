package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.model.Models;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Models.class, remap = false)
public abstract class MixinModels {

    @Shadow
    @Final
    private static Models[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Models[] values() {
        return $VALUES;
    }
}
