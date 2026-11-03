package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.IBlockStatePalette;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockStateContainer.class)
public interface AccessorObserverPalette {
    @Accessor("palette")
    IBlockStatePalette nova$getObserverPalette();
}
