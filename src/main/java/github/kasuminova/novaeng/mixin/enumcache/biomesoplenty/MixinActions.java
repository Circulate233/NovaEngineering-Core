package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.client.gui.GuiBOPConfigureWorld.Actions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Actions.class, remap = false)
public abstract class MixinActions {

    @Shadow
    @Final
    private static Actions[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Actions[] values() {
        return $VALUES;
    }
}
