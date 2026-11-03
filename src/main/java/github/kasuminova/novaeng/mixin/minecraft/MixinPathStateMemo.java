package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.PathStateMemo;
import net.minecraft.block.state.BlockStateContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockStateContainer.StateImplementation.class)
public abstract class MixinPathStateMemo implements PathStateMemo {
    @Unique
    private byte nova$pathKind;

    @Override
    public int nova$pathKind() {
        return nova$pathKind;
    }

    @Override
    public void nova$pathKind(final int kind) {
        nova$pathKind = (byte) kind;
    }
}
