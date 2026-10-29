package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Item.ToolMaterial.class, remap = false)
public abstract class MixinToolMaterial {

    @Shadow
    @Final
    private static Item.ToolMaterial[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Item.ToolMaterial[] values() {
        return $VALUES;
    }
}
