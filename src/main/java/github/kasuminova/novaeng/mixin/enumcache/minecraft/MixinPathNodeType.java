package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.pathfinding.PathNodeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PathNodeType.class, remap = false)
public abstract class MixinPathNodeType {

    @Shadow
    @Final
    private static PathNodeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PathNodeType[] values() {
        return $VALUES;
    }
}
