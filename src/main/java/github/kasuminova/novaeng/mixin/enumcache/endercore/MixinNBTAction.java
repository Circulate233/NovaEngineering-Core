package github.kasuminova.novaeng.mixin.enumcache.endercore;

import info.loenwind.autosave.util.NBTAction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NBTAction.class, remap = false)
public abstract class MixinNBTAction {

    @Shadow
    @Final
    private static NBTAction[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NBTAction[] values() {
        return $VALUES;
    }
}
