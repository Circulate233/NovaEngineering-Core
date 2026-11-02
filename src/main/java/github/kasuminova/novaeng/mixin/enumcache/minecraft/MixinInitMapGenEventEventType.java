package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.event.terraingen.InitMapGenEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = InitMapGenEvent.EventType.class, remap = false)
public abstract class MixinInitMapGenEventEventType {

    @Shadow
    @Final
    private static InitMapGenEvent.EventType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static InitMapGenEvent.EventType[] values() {
        return $VALUES;
    }
}
