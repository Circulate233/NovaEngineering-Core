package github.kasuminova.novaeng.mixin.enumcache.betterp2p;

import com.projecturanus.betterp2p.item.BetterMemoryCardModes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BetterMemoryCardModes.class, remap = false)
public abstract class MixinBetterMemoryCardModes {

    @Shadow
    @Final
    private static BetterMemoryCardModes[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BetterMemoryCardModes[] values() {
        return $VALUES;
    }
}
