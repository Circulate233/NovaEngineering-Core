package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PopulateChunkEvent.Populate.EventType.class, remap = false)
public abstract class MixinPopulateEventType {

    @Shadow
    @Final
    private static PopulateChunkEvent.Populate.EventType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PopulateChunkEvent.Populate.EventType[] values() {
        return $VALUES;
    }
}
