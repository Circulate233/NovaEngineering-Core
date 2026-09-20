package github.kasuminova.novaeng.mixin.ic2;

import github.kasuminova.novaeng.common.blockstate.PropertyTableIndependentState;
import ic2.core.block.state.Ic2BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Identifies IC2 states whose {@code withProperty} implementation exclusively uses IC2's own
 * canonical state index.
 */
@Mixin(value = Ic2BlockState.Ic2BlockStateInstance.class, remap = false)
public abstract class MixinIc2BlockStateInstance implements PropertyTableIndependentState {
}
