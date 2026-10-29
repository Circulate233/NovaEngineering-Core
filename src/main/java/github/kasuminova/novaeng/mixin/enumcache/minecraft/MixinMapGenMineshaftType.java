package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.gen.structure.MapGenMineshaft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MapGenMineshaft.Type.class, remap = false)
public abstract class MixinMapGenMineshaftType {

    @Shadow
    @Final
    private static MapGenMineshaft.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MapGenMineshaft.Type[] values() {
        return $VALUES;
    }
}
