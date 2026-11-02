package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.common.performance.BlockMetadataStates;
import net.minecraft.block.BlockStone;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockStone.class)
public abstract class MixinBlockStoneMetadata {

    @Unique
    private final BlockMetadataStates nova$metadataStates = new BlockMetadataStates();

    @WrapOperation(method = "getStateFromMeta", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/block/state/IBlockState;withProperty(Lnet/minecraft/block/properties/IProperty;Ljava/lang/Comparable;)Lnet/minecraft/block/state/IBlockState;"), require = 1)
    private IBlockState nova$canonicalMetadataState(final IBlockState base, final IProperty<?> property,
                                                    final Comparable<?> value, final Operation<IBlockState> original) {
        if (this.nova$metadataStates == null || property != BlockStone.VARIANT || !(value instanceof BlockStone.EnumType variant)) {
            return original.call(base, property, value);
        }
        final int index = variant.getMetadata();
        IBlockState result = this.nova$metadataStates.get(base, index);
        if (result == null) {
            result = original.call(base, property, value);
            this.nova$metadataStates.put(base, index, result);
        }
        return result;
    }
}
