package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.BlockLightMethods;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Cache material and the class's light dispatch metadata, without freezing mutable light values.
 */
@Mixin(BlockStateContainer.StateImplementation.class)
public abstract class MixinStateImplementationProperties implements IBlockState {

    @Unique
    private Material nova$material;
    @Unique
    private volatile boolean nova$materialCached;
    @Unique
    private BlockLightMethods nova$lightMethods;

    @Redirect(method = "getMaterial()Lnet/minecraft/block/material/Material;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getMaterial(Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/block/material/Material;"),
        require = 1)
    @SuppressWarnings("deprecation")
    private Material nova$cachedMaterial(final Block block, final IBlockState state) {
        if (!this.nova$materialCached) {
            this.nova$material = block.getMaterial(state);
            this.nova$materialCached = true;
        }
        return this.nova$material;
    }

    @Redirect(method = "getLightOpacity(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)I",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getLightOpacity(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)I"),
        remap = false, require = 1)
    private int nova$directLightOpacity(final Block block, final IBlockState state,
                                        final IBlockAccess world, final BlockPos pos) {
        return nova$lightMethods(block).defaultOpacity()
            ? state.getLightOpacity() : block.getLightOpacity(state, world, pos);
    }

    @Redirect(method = "getLightValue(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)I",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getLightValue(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)I"),
        remap = false, require = 1)
    private int nova$directLightValue(final Block block, final IBlockState state,
                                      final IBlockAccess world, final BlockPos pos) {
        return nova$lightMethods(block).defaultValue()
            ? state.getLightValue() : block.getLightValue(state, world, pos);
    }

    @Unique
    private BlockLightMethods nova$lightMethods(final Block block) {
        BlockLightMethods methods = nova$lightMethods;
        if (methods == null) nova$lightMethods = methods = BlockLightMethods.of(block);
        return methods;
    }
}
