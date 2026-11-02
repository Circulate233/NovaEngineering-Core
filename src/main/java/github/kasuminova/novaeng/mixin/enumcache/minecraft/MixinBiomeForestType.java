package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.biome.BiomeForest;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BiomeForest.Type.class, remap = false)
public abstract class MixinBiomeForestType {

    @Shadow
    @Final
    private static BiomeForest.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BiomeForest.Type[] values() {
        return $VALUES;
    }
}
