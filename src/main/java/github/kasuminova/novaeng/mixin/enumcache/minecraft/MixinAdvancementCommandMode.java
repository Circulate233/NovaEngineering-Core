package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.command.AdvancementCommand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AdvancementCommand.Mode.class, remap = false)
public abstract class MixinAdvancementCommandMode {

    @Shadow
    @Final
    private static AdvancementCommand.Mode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AdvancementCommand.Mode[] values() {
        return $VALUES;
    }
}
