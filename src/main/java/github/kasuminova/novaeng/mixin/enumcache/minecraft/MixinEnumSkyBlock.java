package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.EnumSkyBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumSkyBlock.class, remap = false)
public abstract class MixinEnumSkyBlock {

    @Shadow
    @Final
    private static EnumSkyBlock[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumSkyBlock[] values() {
        return $VALUES;
    }
}
