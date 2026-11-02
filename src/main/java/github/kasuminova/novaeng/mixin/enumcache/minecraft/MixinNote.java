package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.event.world.NoteBlockEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NoteBlockEvent.Note.class, remap = false)
public abstract class MixinNote {

    @Shadow
    @Final
    private static NoteBlockEvent.Note[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NoteBlockEvent.Note[] values() {
        return $VALUES;
    }
}
