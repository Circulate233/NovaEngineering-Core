package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.storage.MapDecoration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MapDecoration.Type.class, remap = false)
public abstract class MixinMapDecorationType {

    @Shadow
    @Final
    private static MapDecoration.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MapDecoration.Type[] values() {
        return $VALUES;
    }
}
