package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.client.CPacketRecipeInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CPacketRecipeInfo.Purpose.class, remap = false)
public abstract class MixinPurpose {

    @Shadow
    @Final
    private static CPacketRecipeInfo.Purpose[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CPacketRecipeInfo.Purpose[] values() {
        return $VALUES;
    }
}
