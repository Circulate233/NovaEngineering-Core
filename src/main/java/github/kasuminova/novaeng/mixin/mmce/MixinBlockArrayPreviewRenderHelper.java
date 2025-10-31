package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.client.util.NEWBlockArrayPreviewRenderHelper;
import github.kasuminova.novaeng.mixin.util.BlockArrayPreviewRenderUtils;
import github.kasuminova.novaeng.mixin.util.BlockArrayRenderUtils;
import hellfirepvp.modularmachinery.client.util.BlockArrayPreviewRenderHelper;
import hellfirepvp.modularmachinery.client.util.BlockArrayRenderHelper;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    @Unique
    private EnumFacing n$facing;

    @Shadow
    abstract void renderTranslucentBlocks();

    @Shadow
    protected abstract void updateLayers();

    @Redirect(method = {"hashBlocks", "hasLowerLayer", "updateLayers"}, at = @At(value = "FIELD", target = "Lhellfirepvp/modularmachinery/client/util/BlockArrayPreviewRenderHelper;attachedPosition:Lnet/minecraft/util/math/BlockPos;"))
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
        if (!((Object) this == NEWBlockArrayPreviewRenderHelper.INSTANCE)) {
            this.updateLayers();
        }
    }

    @Unique
    public void n$renderTranslucentBlocks() {
        this.renderTranslucentBlocks();
    }

    @Inject(method = "clearSelection", at = @At("TAIL"))
    public void onClear(CallbackInfo ci) {
        if ((Object) this == NEWBlockArrayPreviewRenderHelper.INSTANCE) {
            NEWBlockArrayPreviewRenderHelper.INSTANCE.clear();
            n$facing = null;
        }
    }

    @Redirect(method = "batchBlocks", at = @At(value = "CONSTANT", args = "classValue=hellfirepvp/modularmachinery/common/block/BlockController"))
    public boolean isFacing(Object instance, Class<?> type) {
        return n$facing != null || instance instanceof BlockController;
    }

    @Redirect(method = "batchBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;getValue(Lnet/minecraft/block/properties/IProperty;)Ljava/lang/Comparable;", remap = true))
    public <T extends Comparable<T>> T setBlockFacing(IBlockState instance, IProperty<T> tiProperty) {
        if (n$facing != null && tiProperty == BlockController.FACING) {
            return (T) n$facing;
        }
        return instance.getValue(tiProperty);
    }

    @Intrinsic
    public EnumFacing getFacing() {
        return n$facing;
    }

    @Intrinsic
    public void setFacing(EnumFacing facing) {
        n$facing = facing;
    }
}