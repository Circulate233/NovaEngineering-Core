package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.biome.BiomeTaiga;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BiomeTaiga.Type.class, remap = false)
public abstract class MixinBiomeTaigaType {

    @Shadow
    @Final
    private static BiomeTaiga.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BiomeTaiga.Type[] values() {
        return $VALUES;
    }
}
