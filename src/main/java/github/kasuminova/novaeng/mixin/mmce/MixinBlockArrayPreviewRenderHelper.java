package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.client.util.NEWBlockArrayPreviewRenderHelper;
import github.kasuminova.novaeng.mixin.util.BlockArrayPreviewRenderUtils;
import github.kasuminova.novaeng.mixin.util.BlockArrayRenderUtils;
import hellfirepvp.modularmachinery.client.util.BlockArrayPreviewRenderHelper;
import hellfirepvp.modularmachinery.client.util.BlockArrayRenderHelper;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockArrayPreviewRenderHelper.class, remap = false)
public abstract class MixinBlockArrayPreviewRenderHelper implements BlockArrayPreviewRenderUtils {

    @Shadow
    @Setter
    @Getter
    private BlockPos attachedPosition;

    @Shadow
    @Getter
    @Setter
    private int renderedLayer;

    @Shadow
    @Getter
    @Setter
    private BlockArrayRenderHelper renderHelper;
    @Shadow
    @Getter
    @Setter
    private BlockArray matchArray;
    @Shadow
    @Getter
    @Setter
    private Vec3i renderHelperOffset;

    @Shadow
    abstract void renderTranslucentBlocks();

    @Shadow
    protected abstract void updateLayers();

    @Redirect(method = {"hashBlocks", "doesPlacedLayerMatch", "hasLowerLayer", "updateLayers"}, at = @At(value = "FIELD", target = "Lhellfirepvp/modularmachinery/client/util/BlockArrayPreviewRenderHelper;attachedPosition:Lnet/minecraft/util/math/BlockPos;"))
    public BlockPos isRenderingComplete(BlockArrayPreviewRenderHelper instance) {
        if (this.renderHelper == null) return this.attachedPosition;
        if ((Object) this instanceof NEWBlockArrayPreviewRenderHelper) {
            var maxY = ((BlockArrayRenderUtils) this.renderHelper).n$getBlocks().getMax().getY();
            if (renderedLayer > maxY) {
                return null;
            }
        }
        return this.attachedPosition;
    }

    @Redirect(method = "batchBlocks", at = @At(value = "FIELD", target = "Lhellfirepvp/modularmachinery/client/util/BlockArrayPreviewRenderHelper;attachedPosition:Lnet/minecraft/util/math/BlockPos;", ordinal = 1))
    public BlockPos isRenderingCompleteB(BlockArrayPreviewRenderHelper instance) {
        if (this.renderHelper == null) return this.attachedPosition;
        if ((Object) this instanceof NEWBlockArrayPreviewRenderHelper) {
            var maxY = ((BlockArrayRenderUtils) this.renderHelper).n$getBlocks().getMax().getY();
            if (renderedLayer > maxY) {
                return null;
            }
        }
        return this.attachedPosition;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lhellfirepvp/modularmachinery/client/util/BlockArrayPreviewRenderHelper;updateLayers()V"))
    private void updateLayers(BlockArrayPreviewRenderHelper instance) {
        if (!((Object) this instanceof NEWBlockArrayPreviewRenderHelper)) {
            this.updateLayers();
        }
    }

    @Unique
    public void n$renderTranslucentBlocks() {
        this.renderTranslucentBlocks();
    }
}