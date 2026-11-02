package github.kasuminova.novaeng.mixin.enumcache.modularui;

import com.cleanroommc.modularui.value.sync.document.DocumentFrameOpcode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DocumentFrameOpcode.class, remap = false)
public abstract class MixinDocumentFrameOpcode {

    @Shadow
    @Final
    private static DocumentFrameOpcode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DocumentFrameOpcode[] values() {
        return $VALUES;
    }
}
