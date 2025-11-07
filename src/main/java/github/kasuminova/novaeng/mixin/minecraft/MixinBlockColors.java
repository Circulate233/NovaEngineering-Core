package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.mixin.util.NovaBlockColors;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraftforge.registries.IRegistryDelegate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(BlockColors.class)
public class MixinBlockColors implements NovaBlockColors {

    @Shadow(remap = false)
    @Final
    @Mutable
    private Map<IRegistryDelegate<Block>, IBlockColor> blockColorMap;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/color/BlockColors;blockColorMap:Ljava/util/Map;", remap = false))
    public void redMap(BlockColors instance, Map<IRegistryDelegate<Block>, IBlockColor> value) {
        this.blockColorMap = new Object2ObjectOpenHashMap<>();
    }

    @Override
    public void n$put(Block block, IBlockColor blockColor) {
        this.blockColorMap.put(block.delegate, blockColor);
    }
}
