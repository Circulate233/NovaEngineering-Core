package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.common.item.ItemJarFilled.JarContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = JarContents.class, remap = false)
public abstract class MixinJarContents {

    @Shadow
    @Final
    private static JarContents[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static JarContents[] values() {
        return $VALUES;
    }
}
