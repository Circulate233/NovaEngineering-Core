package github.kasuminova.novaeng.mixin.enumcache.armourers_workshop;

import moe.plushie.armourers_workshop.common.network.messages.client.MessageClientGuiSkinLibraryCommand.SkinLibraryCommand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SkinLibraryCommand.class, remap = false)
public abstract class MixinSkinLibraryCommand {

    @Shadow
    @Final
    private static SkinLibraryCommand[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SkinLibraryCommand[] values() {
        return $VALUES;
    }
}
