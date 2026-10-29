package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.BiomeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BiomeManager.BiomeType.class, remap = false)
public abstract class MixinBiomeType {

    @Shadow
    @Final
    private static BiomeManager.BiomeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BiomeManager.BiomeType[] values() {
        return $VALUES;
    }
}
