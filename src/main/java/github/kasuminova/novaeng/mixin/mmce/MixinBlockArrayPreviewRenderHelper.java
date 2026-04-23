package github.kasuminova.novaeng.mixin.mmce;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import github.kasuminova.novaeng.client.util.NEWBlockArrayPreviewRenderHelper;
import github.kasuminova.novaeng.mixin.util.BlockArrayPreviewRenderUtils;
import hellfirepvp.modularmachinery.client.util.BlockArrayPreviewRenderHelper;
import hellfirepvp.modularmachinery.client.util.BlockArrayRenderHelper;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.objectweb.asm.Opcodes;
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
    @Unique
    private boolean n$forceDetachedPreview;

    @Shadow
    abstract void renderTranslucentBlocks();

    @Redirect(method = {"hashBlocks", "hasLowerLayer", "updateLayers"}, at = @At(value = "FIELD", target = "Lhellfirepvp/modularmachinery/client/util/BlockArrayPreviewRenderHelper;attachedPosition:Lnet/minecraft/util/math/BlockPos;", opcode = Opcodes.GETFIELD))
    public BlockPos isRenderingComplete(BlockArrayPreviewRenderHelper instance) {
        if (this.renderHelper == null) return this.attachedPosition;
        if ((Object) this == NEWBlockArrayPreviewRenderHelper.INSTANCE && n$forceDetachedPreview) {
            return null;
        }
        return this.attachedPosition;
    }

    @Redirect(method = "batchBlocks", at = @At(value = "FIELD", target = "Lhellfirepvp/modularmachinery/client/util/BlockArrayPreviewRenderHelper;attachedPosition:Lnet/minecraft/util/math/BlockPos;", ordinal = 1, opcode = Opcodes.GETFIELD))
    public BlockPos isRenderingCompleteB(BlockArrayPreviewRenderHelper instance) {
        if (this.renderHelper == null) return this.attachedPosition;
        if ((Object) this == NEWBlockArrayPreviewRenderHelper.INSTANCE && n$forceDetachedPreview) {
            return null;
        }
        return this.attachedPosition;
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
            n$forceDetachedPreview = false;
        }
    }

    @Definition(id = "BlockController", type = BlockController.class)
    @Expression("? instanceof BlockController")
    @ModifyExpressionValue(method = "batchBlocks", at = @At("MIXINEXTRAS:EXPRESSION"), remap = false)
    private boolean n$matchControllerPreview(final boolean original) {
        return n$facing != null || original;
    }

    @Definition(id = "getValue", method = "Lnet/minecraft/block/state/IBlockState;getValue(Lnet/minecraft/block/properties/IProperty;)Ljava/lang/Comparable;")
    @Definition(id = "FACING", field = "Lhellfirepvp/modularmachinery/common/block/BlockController;FACING:Lnet/minecraft/block/properties/PropertyEnum;", remap = false)
    @Expression("?.getValue(FACING)")
    @ModifyExpressionValue(method = "batchBlocks", at = @At("MIXINEXTRAS:EXPRESSION"), remap = false)
    private <T extends Comparable<T>> T n$setPreviewFacing(final T original) {
        if (n$facing != null) {
            return (T) n$facing;
        }
        return original;
    }

    @Intrinsic
    public EnumFacing getFacing() {
        return n$facing;
    }

    @Intrinsic
    public void setFacing(EnumFacing facing) {
        n$facing = facing;
    }

    @Intrinsic
    public boolean isForceDetachedPreview() {
        return n$forceDetachedPreview;
    }

    @Intrinsic
    public void setForceDetachedPreview(boolean forceDetachedPreview) {
        n$forceDetachedPreview = forceDetachedPreview;
    }
}
