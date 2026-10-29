package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TickEvent.Phase.class, remap = false)
public abstract class MixinPhase {

    @Shadow
    @Final
    private static TickEvent.Phase[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TickEvent.Phase[] values() {
        return $VALUES;
    }
}
