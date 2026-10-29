package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.event.world.NoteBlockEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NoteBlockEvent.Instrument.class, remap = false)
public abstract class MixinInstrument {

    @Shadow
    @Final
    private static NoteBlockEvent.Instrument[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NoteBlockEvent.Instrument[] values() {
        return $VALUES;
    }
}
