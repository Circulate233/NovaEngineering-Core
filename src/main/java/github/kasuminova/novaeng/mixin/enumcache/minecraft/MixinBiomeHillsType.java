package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.biome.BiomeHills;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BiomeHills.Type.class, remap = false)
public abstract class MixinBiomeHillsType {

    @Shadow
    @Final
    private static BiomeHills.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BiomeHills.Type[] values() {
        return $VALUES;
    }
}
