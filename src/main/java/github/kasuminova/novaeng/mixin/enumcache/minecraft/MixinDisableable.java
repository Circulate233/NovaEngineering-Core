package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.common.ModContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModContainer.Disableable.class, remap = false)
public abstract class MixinDisableable {

    @Shadow
    @Final
    private static ModContainer.Disableable[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModContainer.Disableable[] values() {
        return $VALUES;
    }
}
