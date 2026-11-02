package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DecorateBiomeEvent.Decorate.EventType.class, remap = false)
public abstract class MixinDecorateEventType {

    @Shadow
    @Final
    private static DecorateBiomeEvent.Decorate.EventType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DecorateBiomeEvent.Decorate.EventType[] values() {
        return $VALUES;
    }
}
