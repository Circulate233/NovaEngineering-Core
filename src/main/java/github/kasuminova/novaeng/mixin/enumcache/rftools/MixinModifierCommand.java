package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.items.modifier.ModifierCommand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModifierCommand.class, remap = false)
public abstract class MixinModifierCommand {

    @Shadow
    @Final
    private static ModifierCommand[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModifierCommand[] values() {
        return $VALUES;
    }
}
