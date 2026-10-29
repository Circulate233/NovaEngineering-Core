package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityPlayer.EnumChatVisibility.class, remap = false)
public abstract class MixinEnumChatVisibility {

    @Shadow
    @Final
    private static EntityPlayer.EnumChatVisibility[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityPlayer.EnumChatVisibility[] values() {
        return $VALUES;
    }
}
