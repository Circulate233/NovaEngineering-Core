package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.integration.DECoreBindingIndex;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEnergyHatch.class, remap = false)
public abstract class MixinTileEnergyHatchDEBinding extends TileColorableMachineComponent implements DECoreBindingIndex.TileEnergyHatchBinding {

    @Shadow
    protected BlockPos foundCore;

    @Override
    public void novaeng$setFoundCore(final BlockPos pos) {
        this.foundCore = pos == null ? null : pos.toImmutable();
    }

    /**
     * Deliberately empty: the hatch used to scan for a DE core from its own tick. The core is now bound through
     * {@link DECoreBindingIndex}, which listens to the circulation-networks tile entity lifecycle events, so the scan
     * must not run. The visibility has to match the target method, else Mixin only warns and silently upgrades it.
     */
    @Overwrite
    protected void findCore() {

    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        this.foundCore = null;
    }

}
