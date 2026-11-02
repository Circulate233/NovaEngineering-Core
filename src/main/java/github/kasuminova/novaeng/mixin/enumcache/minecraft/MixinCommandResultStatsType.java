package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.command.CommandResultStats;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CommandResultStats.Type.class, remap = false)
public abstract class MixinCommandResultStatsType {

    @Shadow
    @Final
    private static CommandResultStats.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CommandResultStats.Type[] values() {
        return $VALUES;
    }
}
