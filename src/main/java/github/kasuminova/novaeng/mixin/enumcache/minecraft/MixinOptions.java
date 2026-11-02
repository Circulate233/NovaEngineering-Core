package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GameSettings.Options.class, remap = false)
public abstract class MixinOptions {

    @Shadow
    @Final
    private static GameSettings.Options[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GameSettings.Options[] values() {
        return $VALUES;
    }
}
