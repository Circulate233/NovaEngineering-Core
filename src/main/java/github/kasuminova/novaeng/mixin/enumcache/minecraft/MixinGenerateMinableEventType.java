package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.event.terraingen.OreGenEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = OreGenEvent.GenerateMinable.EventType.class, remap = false)
public abstract class MixinGenerateMinableEventType {

    @Shadow
    @Final
    private static OreGenEvent.GenerateMinable.EventType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static OreGenEvent.GenerateMinable.EventType[] values() {
        return $VALUES;
    }
}
