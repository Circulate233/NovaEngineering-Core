package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.oredict.RecipeSorter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RecipeSorter.Category.class, remap = false)
public abstract class MixinCategory {

    @Shadow
    @Final
    private static RecipeSorter.Category[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RecipeSorter.Category[] values() {
        return $VALUES;
    }
}
