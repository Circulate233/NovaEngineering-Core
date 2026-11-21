package github.kasuminova.novaeng.mixin.geometry.actinium;

import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.Item;
import net.minecraftforge.registries.IRegistryDelegate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ItemColors.class)
public interface AccessorGeometryItemColors {
    // Match Forge's Map field descriptor; the accessor does not allocate a collection.
    @Accessor(value = "itemColorMap", remap = false)
    Map<IRegistryDelegate<Item>, IItemColor> novaeng$colors();
}
