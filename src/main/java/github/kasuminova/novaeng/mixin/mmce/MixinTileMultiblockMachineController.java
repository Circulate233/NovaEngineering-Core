package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.hypernet.old.HyperNetCaches;
import github.kasuminova.novaeng.common.machine.MachineSpecial;
import github.kasuminova.novaeng.common.registry.RegistryMachineSpecial;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(TileMultiblockMachineController.class)
public abstract class MixinTileMultiblockMachineController extends TileEntity {

    @Nullable
    @Shadow(remap = false)
    public abstract DynamicMachine getFoundMachine();

    @Shadow(remap = false)
    public abstract TileMultiblockMachineController getController();

    @Unique
    private DynamicMachine novaeng$specialKey;

    @Unique
    private int novaeng$specialVersion = -1;

    @Unique
    private MachineSpecial novaeng$special;

    @Inject(method = "doRestrictedTick", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectUpdate(final CallbackInfo ci) {
        DynamicMachine foundMachine = getFoundMachine();
        if (foundMachine == null) {
            return;
        }
        MachineSpecial specialMachine = novaeng$resolveSpecial(foundMachine);

        boolean client = getWorld().isRemote;
        if (client) {
            ci.cancel();
        }

        if (specialMachine != null) {
            if (client) {
                specialMachine.onClientTick(getController());
            } else {
                specialMachine.onSyncTick(getController());
            }
        }
    }

    /**
     * The machine of a controller only changes when its structure is re-checked, so the registry
     * lookup is remembered until either that machine or the registry itself changes.
     */
    @Unique
    private MachineSpecial novaeng$resolveSpecial(final DynamicMachine machine) {
        final int version = RegistryMachineSpecial.version();
        if (machine == this.novaeng$specialKey && version == this.novaeng$specialVersion) {
            return this.novaeng$special;
        }
        final MachineSpecial resolved = RegistryMachineSpecial.getSpecialMachine(machine.getRegistryName());
        this.novaeng$specialKey = machine;
        this.novaeng$specialVersion = version;
        this.novaeng$special = resolved;
        return resolved;
    }

    @Inject(method = "resetMachine", at = @At("HEAD"), remap = false)
    private void onResetMachine(final boolean clearData, final CallbackInfo ci) {
        if (clearData) {
            novaeng_hypernet$removeCache();
        }
    }

    @Inject(method = "invalidate", at = @At("HEAD"))
    private void onInvalidate(final CallbackInfo ci) {
        novaeng_hypernet$removeCache();
    }

    @Inject(method = "onChunkUnload", at = @At("RETURN"), remap = false)
    public void injectOnChunkUnload(final CallbackInfo ci) {
        novaeng_hypernet$removeCache();
    }

    /**
     * Forgets this controller in every controller-keyed HyperNet cache.
     *
     * <p>Unconditional on purpose. This used to only fire when
     * {@code RegistryHyperNet.isHyperNetSupported(getFoundMachine())} held, which leaks
     * exactly when it matters: a tile that is invalidating has often already lost its
     * found machine, so the lookup returns null, the guard fails, and the cache keeps
     * the tile - and through its {@code world} field the entire World - forever. The
     * caches are identity-keyed maps; removing a key that was never in one costs a
     * lookup and nothing else.</p>
     */
    @Unique
    private void novaeng_hypernet$removeCache() {
        HyperNetCaches.remove((TileMultiblockMachineController) (Object) this);
    }
}
