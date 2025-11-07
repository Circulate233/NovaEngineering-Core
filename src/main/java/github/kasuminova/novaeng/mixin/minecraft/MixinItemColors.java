package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.mixin.util.NovaItemColors;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.Item;
import net.minecraftforge.registries.IRegistryDelegate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(ItemColors.class)
public class MixinItemColors implements NovaItemColors {

    @Shadow(remap = false)
    @Final
    @Mutable
    private Map<IRegistryDelegate<Item>, IItemColor> itemColorMap;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/color/ItemColors;itemColorMap:Ljava/util/Map;", remap = false))
    public void redMap(ItemColors instance, Map<IRegistryDelegate<Item>, IItemColor> value) {
        this.itemColorMap = new Object2ObjectOpenHashMap<>();
    }

    @Override
    public void n$put(Item item, IItemColor itemColor) {
        this.itemColorMap.put(item.delegate, itemColor);
    }
}
