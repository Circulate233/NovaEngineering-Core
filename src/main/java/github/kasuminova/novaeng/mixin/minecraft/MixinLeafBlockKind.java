package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.LeafBlockKindAccess;
import github.kasuminova.novaeng.common.performance.LeafBlockKinds;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class MixinLeafBlockKind implements LeafBlockKindAccess {
    @Unique
    private int nova$leafKind;

    @Inject(method = "<init>(Lnet/minecraft/block/material/Material;Lnet/minecraft/block/material/MapColor;)V", at = @At("RETURN"), require = 1)
    private void nova$classifyLeafBehavior(final CallbackInfo ci) {
        nova$leafKind = LeafBlockKinds.classify(getClass());
    }

    @Override
    public int nova$leafKind() {
        return nova$leafKind;
    }
}
