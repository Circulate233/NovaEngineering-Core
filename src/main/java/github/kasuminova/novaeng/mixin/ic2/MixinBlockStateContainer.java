package github.kasuminova.novaeng.mixin.ic2;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.common.blockstate.PropertyTableIndependentState;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/**
 * Avoids building Minecraft's redundant state-transition tables for implementations that provide
 * an independent canonical transition lookup.
 */
@Mixin(BlockStateContainer.class)
public abstract class MixinBlockStateContainer {

    @WrapOperation(
        method = "<init>(Lnet/minecraft/block/Block;[Lnet/minecraft/block/properties/IProperty;Lcom/google/common/collect/ImmutableMap;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/state/BlockStateContainer$StateImplementation;buildPropertyValueTable(Ljava/util/Map;)V"),
        require = 1
    )
    private void nova$skipUnusedPropertyTable(
        final BlockStateContainer.StateImplementation state,
        final Map<Map<IProperty<?>, Comparable<?>>,
            BlockStateContainer.StateImplementation> map,
        final Operation<Void> original
    ) {
        if (!(state instanceof PropertyTableIndependentState)) {
            original.call(state, map);
        }
    }
}
