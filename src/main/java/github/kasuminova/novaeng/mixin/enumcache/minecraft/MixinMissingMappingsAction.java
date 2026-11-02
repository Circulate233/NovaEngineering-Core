package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.event.RegistryEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RegistryEvent.MissingMappings.Action.class, remap = false)
public abstract class MixinMissingMappingsAction {

    @Shadow
    @Final
    private static RegistryEvent.MissingMappings.Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RegistryEvent.MissingMappings.Action[] values() {
        return $VALUES;
    }
}
