package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.command.AdvancementCommand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AdvancementCommand.ActionType.class, remap = false)
public abstract class MixinActionType {

    @Shadow
    @Final
    private static AdvancementCommand.ActionType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AdvancementCommand.ActionType[] values() {
        return $VALUES;
    }
}
