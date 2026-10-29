package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.datafix.FixTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FixTypes.class, remap = false)
public abstract class MixinFixTypes {

    @Shadow
    @Final
    private static FixTypes[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FixTypes[] values() {
        return $VALUES;
    }
}
