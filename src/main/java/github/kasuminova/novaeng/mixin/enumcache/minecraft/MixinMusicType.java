package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.audio.MusicTicker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MusicTicker.MusicType.class, remap = false)
public abstract class MixinMusicType {

    @Shadow
    @Final
    private static MusicTicker.MusicType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MusicTicker.MusicType[] values() {
        return $VALUES;
    }
}
