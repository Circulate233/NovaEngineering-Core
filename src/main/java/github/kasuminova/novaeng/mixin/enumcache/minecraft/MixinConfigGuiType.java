package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.client.config.ConfigGuiType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConfigGuiType.class, remap = false)
public abstract class MixinConfigGuiType {

    @Shadow
    @Final
    private static ConfigGuiType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConfigGuiType[] values() {
        return $VALUES;
    }
}
