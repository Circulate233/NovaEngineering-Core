package github.kasuminova.novaeng.mixin.enumcache.thermaldynamics;

import cofh.thermaldynamics.block.BlockDuct.ConnectionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConnectionType.class, remap = false)
public abstract class MixinConnectionType {

    @Shadow
    @Final
    private static ConnectionType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConnectionType[] values() {
        return $VALUES;
    }
}
