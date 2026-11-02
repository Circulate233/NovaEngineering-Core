package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.Session;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Session.Type.class, remap = false)
public abstract class MixinSessionType {

    @Shadow
    @Final
    private static Session.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Session.Type[] values() {
        return $VALUES;
    }
}
