package github.kasuminova.novaeng.mixin.enumcache.openmodularturrets;

import omtteam.openmodularturrets.tileentity.TurretBase.commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = commands.class, remap = false)
public abstract class MixinCommands {

    @Shadow
    @Final
    private static commands[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static commands[] values() {
        return $VALUES;
    }
}
