package github.kasuminova.novaeng.mixin.enumcache.armourers_workshop;

import moe.plushie.armourers_workshop.common.network.messages.server.MessageServerClientCommand.CommandType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CommandType.class, remap = false)
public abstract class MixinCommandType {

    @Shadow
    @Final
    private static CommandType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CommandType[] values() {
        return $VALUES;
    }
}
