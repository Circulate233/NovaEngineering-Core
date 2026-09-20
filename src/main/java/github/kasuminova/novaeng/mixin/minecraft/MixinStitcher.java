package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Final;

import java.util.List;

/**
 * @author circulation
 * @reason allocateSlot 对每个 sprite 线性尝试全部顶层 Slot 并重复计算 Holder 尺寸，
 *       将尺寸判断提升到循环外并跳过必然失败的 addSlot 调用，布局结果不变。
 */
@Mixin(Stitcher.class)
public abstract class MixinStitcher {

    @Shadow
    @Final
    private List<Stitcher.Slot> stitchSlots;

    @Shadow
    protected abstract boolean expandAndAllocateSlot(Stitcher.Holder holder);

    /**
     * @author circulation
     * @reason 提升尺寸判断，跳过装不下的 Slot，遍历顺序与判定结果与原版完全一致
     */
    @Overwrite
    private boolean allocateSlot(final Stitcher.Holder holder) {
        final TextureAtlasSprite sprite = holder.getAtlasSprite();
        final boolean canRotate = sprite.getIconWidth() != sprite.getIconHeight();
        final int widthA = holder.getWidth();
        final int heightA = holder.getHeight();
        holder.rotate();
        final int widthB = holder.getWidth();
        final int heightB = holder.getHeight();
        holder.rotate();

        for (int i = 0; i < this.stitchSlots.size(); ++i) {
            final Stitcher.Slot slot = this.stitchSlots.get(i);
            final int slotWidth = ((AccessorStitcherSlot) slot).nova$getWidth();
            final int slotHeight = ((AccessorStitcherSlot) slot).nova$getHeight();
            if (widthA <= slotWidth && heightA <= slotHeight && slot.addSlot(holder)) {
                return true;
            }
            if (canRotate && widthB <= slotWidth && heightB <= slotHeight) {
                holder.rotate();
                if (slot.addSlot(holder)) {
                    return true;
                }
                holder.rotate();
            }
        }

        return this.expandAndAllocateSlot(holder);
    }
}
