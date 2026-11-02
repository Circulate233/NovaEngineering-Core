package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.event.world.NoteBlockEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NoteBlockEvent.Octave.class, remap = false)
public abstract class MixinOctave {

    @Shadow
    @Final
    private static NoteBlockEvent.Octave[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NoteBlockEvent.Octave[] values() {
        return $VALUES;
    }
}
