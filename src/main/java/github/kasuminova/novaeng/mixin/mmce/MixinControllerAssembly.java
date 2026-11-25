package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.assembly.AssemblyController;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntityRestrictedTick;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileMultiblockMachineController.class, remap = false)
public abstract class MixinControllerAssembly extends TileEntityRestrictedTick implements AssemblyController {
    @Shadow
    protected int lastStructureCheckTick;

    @Shadow
    protected abstract boolean canCheckStructure();

    @Shadow
    protected abstract void resetMachine(boolean clearData);

    @Shadow
    protected abstract void updateStatedMachineComponent(boolean working);

    @Shadow
    public abstract void notifyStructureFormedState(boolean formed);

    @Unique
    private boolean nova$dismantling;

    @Override
    public boolean nova$beginDismantling() {
        if (nova$dismantling) return false;
        nova$dismantling = true;
        try {
            // resetMachine queues this update in async mode, after it has discarded the pattern.
            updateStatedMachineComponent(false);
            resetMachine(true);
            notifyStructureFormedState(false);
            markForUpdate();
            return true;
        } catch (RuntimeException | Error failure) {
            nova$dismantling = false;
            throw failure;
        }
    }

    @Override
    public void nova$endDismantling() {
        nova$dismantling = false;
        lastStructureCheckTick = -1;
    }

    @Redirect(method = {"doStructureCheck", "checkStructure", "updateComponents"}, at = @At(value = "INVOKE",
        target = "Lhellfirepvp/modularmachinery/common/tiles/base/TileMultiblockMachineController;canCheckStructure()Z"), require = 3)
    private boolean nova$holdUnformed(final TileMultiblockMachineController controller) {
        return !nova$dismantling && canCheckStructure();
    }
}
