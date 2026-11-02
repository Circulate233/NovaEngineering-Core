package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.BossInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BossInfo.Overlay.class, remap = false)
public abstract class MixinOverlay {

    @Shadow
    @Final
    private static BossInfo.Overlay[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BossInfo.Overlay[] values() {
        return $VALUES;
    }
}
