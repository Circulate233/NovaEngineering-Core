package github.kasuminova.novaeng.mixin.enumcache.gravisuite;

import com.chocohead.gravisuite.items.ItemCraftingThings.CraftingTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftingTypes.class, remap = false)
public abstract class MixinCraftingTypes {

    @Shadow
    @Final
    private static CraftingTypes[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CraftingTypes[] values() {
        return $VALUES;
    }
}
