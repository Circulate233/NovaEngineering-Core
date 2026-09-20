package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.texture.SlotCapacityFrontier;
import github.kasuminova.novaeng.client.texture.SlotCapacityFrontierImpl;
import github.kasuminova.novaeng.client.texture.StitcherSlotCapacity;
import net.minecraft.client.renderer.texture.Stitcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains an exact Pareto frontier for each Stitcher slot subtree and rejects recursive searches
 * only when no existing leaf can contain the holder in its current rotation.
 */
@Mixin(Stitcher.Slot.class)
public abstract class MixinStitcherSlot implements StitcherSlotCapacity {

    @Shadow
    @Final
    private int width;

    @Shadow
    @Final
    private int height;

    @Shadow
    private List<Stitcher.Slot> subSlots;

    @Shadow
    private Stitcher.Holder holder;

    @Unique
    private SlotCapacityFrontier nova$capacityFrontier;

    @Inject(method = "addSlot", at = @At("HEAD"), cancellable = true, require = 1)
    private void nova$rejectImpossibleSubtree(final Stitcher.Holder holderIn,
                                               final CallbackInfoReturnable<Boolean> cir) {
        if (!this.nova$getCapacityFrontier().canFit(holderIn.getWidth(), holderIn.getHeight())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addSlot", at = @At("RETURN"), require = 1)
    private void nova$refreshFrontierAfterInsertion(final Stitcher.Holder holderIn,
                                                     final CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            this.nova$rebuildCapacityFrontier();
        }
    }

    /**
     * Lazily derives initial state after construction or an external cached-tree restoration.
     *
     * @return the exact current frontier for this slot subtree
     */
    @Override
    public SlotCapacityFrontier nova$getCapacityFrontier() {
        if (this.nova$capacityFrontier == null) {
            this.nova$rebuildCapacityFrontier();
        }
        return this.nova$capacityFrontier;
    }

    @Unique
    private void nova$rebuildCapacityFrontier() {
        if (this.holder != null) {
            this.nova$capacityFrontier = SlotCapacityFrontierImpl.empty();
            return;
        }
        if (this.subSlots == null || this.subSlots.isEmpty()) {
            this.nova$capacityFrontier = SlotCapacityFrontierImpl.freeRectangle(this.width, this.height);
            return;
        }

        final List<SlotCapacityFrontier> childFrontiers = new ArrayList<>(this.subSlots.size());
        for (final Stitcher.Slot child : this.subSlots) {
            childFrontiers.add(((StitcherSlotCapacity) child).nova$getCapacityFrontier());
        }
        this.nova$capacityFrontier = SlotCapacityFrontierImpl.merge(childFrontiers);
    }
}
