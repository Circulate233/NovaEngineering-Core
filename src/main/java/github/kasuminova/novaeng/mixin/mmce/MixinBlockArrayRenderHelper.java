package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.mixin.util.BlockArrayRenderUtils;
import hellfirepvp.modularmachinery.client.util.BlockArrayRenderHelper;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockArrayRenderHelper.class, remap = false)
public class MixinBlockArrayRenderHelper implements BlockArrayRenderUtils {

    @Final
    @Shadow
    private BlockArray blocks;

    @Intrinsic
    public BlockArray n$getBlocks() {
        return blocks;
    }
}
