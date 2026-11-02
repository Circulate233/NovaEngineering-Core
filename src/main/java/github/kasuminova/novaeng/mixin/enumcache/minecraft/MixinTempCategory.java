package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Biome.TempCategory.class, remap = false)
public abstract class MixinTempCategory {

    @Shadow
    @Final
    private static Biome.TempCategory[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Biome.TempCategory[] values() {
        return $VALUES;
    }
}
