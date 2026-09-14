package github.kasuminova.novaeng.mixin.ae2;

import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.me.cluster.implementations.CraftingCPUCluster;
import ae2.me.helpers.MachineSource;
import ae2.tile.crafting.ICraftingCPUTileEntity;
import github.kasuminova.mmce.common.util.TimeRecorder;
import github.kasuminova.novaeng.common.block.ecotech.ecalculator.prop.Levels;
import github.kasuminova.novaeng.common.ecalculator.ECPUCluster;
import github.kasuminova.novaeng.common.tile.ecotech.ecalculator.ECalculatorController;
import github.kasuminova.novaeng.common.tile.ecotech.ecalculator.ECalculatorMEChannel;
import github.kasuminova.novaeng.common.tile.ecotech.ecalculator.ECalculatorThreadCore;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCPUCluster implements ECPUCluster {

    @Unique
    private final TimeRecorder novaeng_ec$timeRecorder = new TimeRecorder();
    @Unique
    private final TimeRecorder novaeng_ec$parallelismRecorder = new TimeRecorder();
    @Unique
    private ECalculatorThreadCore novaeng_ec$core;
    @Unique
    private ECalculatorController novaeng_ec$virtualCPUOwner;
    @Unique
    private long novaeng_ec$usedExtraStorage;

    @Shadow
    protected long storage;
    @Shadow
    protected int accelerator;
    @Shadow
    protected boolean destroyed;
    @Shadow
    protected MachineSource machineSrc;

    @Shadow
    public abstract IGrid getGrid();

    @Shadow
    public abstract World getLevel();

    @Inject(method = "destroy", at = @At("HEAD"))
    private void injectDestroy(final CallbackInfo ci) {
        if (!this.destroyed && this.novaeng_ec$core != null) {
            this.novaeng_ec$core.onCPUDestroyed((CraftingCPUCluster) (Object) this);
        }
    }

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void injectIsActive(final CallbackInfoReturnable<Boolean> cir) {
        ECalculatorController controller = this.novaeng_ec$getOwner();
        if (controller != null) {
            ECalculatorMEChannel channel = controller.getChannel();
            cir.setReturnValue(channel != null && channel.getMainNode().isActive());
        }
    }

    @Inject(method = "getGrid", at = @At("HEAD"), cancellable = true)
    private void injectGetGrid(final CallbackInfoReturnable<IGrid> cir) {
        ECalculatorController controller = this.novaeng_ec$getOwner();
        if (controller == null) {
            return;
        }

        ECalculatorMEChannel channel = controller.getChannel();
        if (channel == null) {
            cir.setReturnValue(null);
            return;
        }

        IGridNode node = channel.getMainNode().getNode();
        cir.setReturnValue(node == null ? null : node.grid());
    }

    @Inject(method = "getCore", at = @At("HEAD"), cancellable = true)
    private void injectGetCore(final CallbackInfoReturnable<ICraftingCPUTileEntity> cir) {
        if (this.novaeng_ec$getOwner() != null) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getLevel", at = @At("HEAD"), cancellable = true)
    private void injectGetLevel(final CallbackInfoReturnable<World> cir) {
        ECalculatorController controller = this.novaeng_ec$getOwner();
        if (controller != null) {
            cir.setReturnValue(controller.getWorld());
        }
    }

    @Inject(method = "markDirty", at = @At("HEAD"), cancellable = true)
    private void injectMarkDirty(final CallbackInfo ci) {
        if (this.novaeng_ec$core != null) {
            this.novaeng_ec$core.markNoUpdateSync();
            ci.cancel();
        } else if (this.novaeng_ec$virtualCPUOwner != null) {
            this.novaeng_ec$virtualCPUOwner.markNoUpdateSync();
            ci.cancel();
        }
    }

    @Unique
    private ECalculatorController novaeng_ec$getOwner() {
        if (this.novaeng_ec$core != null) {
            return this.novaeng_ec$core.getController();
        }
        return this.novaeng_ec$virtualCPUOwner;
    }

    @Unique
    @Override
    public void novaeng_ec$setAvailableStorage(final long availableStorage) {
        this.storage = availableStorage;
    }

    @Unique
    @Override
    public void novaeng_ec$setAccelerators(final int accelerators) {
        this.accelerator = accelerators;
    }

    @Unique
    @Override
    public ECalculatorThreadCore novaeng_ec$getController() {
        return this.novaeng_ec$core;
    }

    @Unique
    @Override
    public void novaeng_ec$setThreadCore(final ECalculatorThreadCore threadCore) {
        this.novaeng_ec$core = threadCore;
        ECalculatorController controller = threadCore.getController();
        if (controller != null && controller.getChannel() != null) {
            this.machineSrc = new MachineSource(controller.getChannel());
        }
    }

    @Unique
    @Override
    public void novaeng_ec$setVirtualCPUOwner(@Nullable final ECalculatorController virtualCPUOwner) {
        this.novaeng_ec$virtualCPUOwner = virtualCPUOwner;
        if (virtualCPUOwner != null && virtualCPUOwner.getChannel() != null) {
            this.machineSrc = new MachineSource(virtualCPUOwner.getChannel());
        }
    }

    @Unique
    @Override
    public Levels novaeng_ec$getControllerLevel() {
        ECalculatorController controller = this.novaeng_ec$getOwner();
        return controller == null ? null : controller.getLevel();
    }

    @Unique
    @Override
    public void novaeng_ec$setUsedExtraStorage(final long usedExtraStorage) {
        this.novaeng_ec$usedExtraStorage = usedExtraStorage;
    }

    @Unique
    @Override
    public long novaeng_ec$getUsedExtraStorage() {
        return this.novaeng_ec$usedExtraStorage;
    }

    @Unique
    @Override
    public void novaeng_ec$markDestroyed() {
        this.destroyed = true;
    }

    @Unique
    @Override
    public TimeRecorder novaeng_ec$getTimeRecorder() {
        return this.novaeng_ec$timeRecorder;
    }

    @Unique
    @Override
    public TimeRecorder novaeng_ec$getParallelismRecorder() {
        return this.novaeng_ec$parallelismRecorder;
    }
}
