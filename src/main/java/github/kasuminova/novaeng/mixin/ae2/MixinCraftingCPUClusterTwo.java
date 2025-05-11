package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.crafting.MECraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCPUClusterTwo {

    @Shadow
    private long lastTime;

    @Shadow
    protected abstract void postChange(IAEItemStack diff, IActionSource src);

    @Shadow protected abstract void postCraftingStatusChange(IAEItemStack diff);

    @Shadow
    private int remainingOperations;
    @Unique
    private boolean r$isMEPatternProvider = false;

    @Unique
    private long r$craftingFrequency = 0;

    // TODO: 为样板供应器增加一个升级项目，用于允许忽略并行处理单元，并且默认上限被并行处理单元限制
    @Redirect(method = "executeCrafting",at = @At(value = "INVOKE", target = "Ljava/util/Map$Entry;getKey()Ljava/lang/Object;"))
    private Object getKeyR(Map.Entry<ICraftingPatternDetails,AccessorTaskProgress> instance) {
        var key = instance.getKey();
        long max = 0;
        for (IAEItemStack stack : key.getCondensedInputs()) {
            long size = stack.getStackSize();
            if (size > max) max = size;
        }
        this.r$craftingFrequency = instance.getValue().getValue();
        //this.r$craftingFrequency = Math.min(this.remainingOperations,this.r$craftingFrequency);
        if (max * this.r$craftingFrequency > Integer.MAX_VALUE){
            this.r$craftingFrequency = Integer.MAX_VALUE / max;
        }
        return key;
    }

    @Redirect(method = "executeCrafting",at = @At(value = "INVOKE", target = "Lappeng/api/networking/crafting/ICraftingMedium;isBusy()Z"))
    private boolean isBusyR(ICraftingMedium instance) {
        if (instance instanceof MEPatternProvider mep){
            this.r$isMEPatternProvider = mep.getWorkMode() == MEPatternProvider.WorkModeSetting.DEFAULT
                    || mep.getWorkMode() == MEPatternProvider.WorkModeSetting.ENHANCED_BLOCKING_MODE;
        }
        return instance.isBusy();
    }

    @Redirect(method = "executeCrafting",at = @At(value = "INVOKE", target = "Lappeng/api/networking/energy/IEnergyGrid;extractAEPower(DLappeng/api/config/Actionable;Lappeng/api/config/PowerMultiplier;)D"))
    private double extractAEPowerR(IEnergyGrid eg, double v, Actionable actionable, PowerMultiplier powerMultiplier) {
        if (this.r$isMEPatternProvider) {
            var sum = v * this.r$craftingFrequency;
            var o = eg.extractAEPower(sum,Actionable.SIMULATE,powerMultiplier);
            if (o < sum - 0.01) {
                long s = (long) (o / sum * this.r$craftingFrequency);
                this.r$craftingFrequency = s;
                if (s < 1) {
                    return eg.extractAEPower(v,actionable,powerMultiplier);
                } else {
                    return eg.extractAEPower(v * s,Actionable.SIMULATE,powerMultiplier);
                }
            }
            return o;
        } else {
            return eg.extractAEPower(v,actionable,powerMultiplier);
        }
    }

    @Redirect(method = "executeCrafting",at = @At(value = "INVOKE", target = "Lappeng/crafting/MECraftingInventory;extractItems(Lappeng/api/storage/data/IAEItemStack;Lappeng/api/config/Actionable;Lappeng/api/networking/security/IActionSource;)Lappeng/api/storage/data/IAEItemStack;"))
    private IAEItemStack extractItemsR(MECraftingInventory instance, IAEItemStack request, Actionable mode, IActionSource src) {
        if (this.r$isMEPatternProvider){
            var i = request.copy().setStackSize(request.getStackSize() * this.r$craftingFrequency);
            return instance.extractItems(i, mode, src);
        }
        return instance.extractItems(request, mode, src);
    }

    @Redirect(
            method = "executeCrafting",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/me/cluster/implementations/CraftingCPUCluster;postChange(Lappeng/api/storage/data/IAEItemStack;Lappeng/api/networking/security/IActionSource;)V",
                    ordinal = 1
            )
    )
    private void postChangeR1(CraftingCPUCluster instance, IAEItemStack receiver, IActionSource single) {
        if (this.r$isMEPatternProvider) {
            var i = receiver.copy().setStackSize(receiver.getStackSize() * this.r$craftingFrequency);
            this.postChange(i, single);
        } else {
            this.postChange(receiver,single);
        }
    }

    @Redirect(
            method = "executeCrafting",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/me/cluster/implementations/CraftingCPUCluster;postChange(Lappeng/api/storage/data/IAEItemStack;Lappeng/api/networking/security/IActionSource;)V",
                    ordinal = 2
            )
    )
    private void postChangeR2(CraftingCPUCluster instance, IAEItemStack receiver, IActionSource single) {
        if (this.r$isMEPatternProvider) {
            var i = receiver.copy().setStackSize(receiver.getStackSize() * this.r$craftingFrequency);
            this.postChange(i, single);
        } else {
            this.postChange(receiver,single);
        }
    }

    @Redirect(method = "executeCrafting",at = @At(value = "INVOKE", target = "Lappeng/api/storage/data/IItemList;add(Lappeng/api/storage/data/IAEStack;)V",ordinal = 0))
    private void addR(IItemList<IAEItemStack> instance, IAEStack<IAEItemStack> iaeStack) {
        if (!this.r$isMEPatternProvider){
            instance.add((IAEItemStack) iaeStack);
        } else {
            iaeStack.setStackSize(iaeStack.getStackSize() * this.r$craftingFrequency);
            instance.add((IAEItemStack) iaeStack);
        }
    }

    @Redirect(method = "executeCrafting",at = @At(value = "INVOKE", target = "Lappeng/me/cluster/implementations/CraftingCPUCluster;postCraftingStatusChange(Lappeng/api/storage/data/IAEItemStack;)V",ordinal = 0))
    private void postCraftingStatusChangeR(CraftingCPUCluster instance, IAEItemStack iaeStack) {
        if (!this.r$isMEPatternProvider){
            this.postCraftingStatusChange(iaeStack);
        } else {
            iaeStack.setStackSize(iaeStack.getStackSize() * this.r$craftingFrequency);
            this.postCraftingStatusChange(iaeStack);
        }
    }

    @Redirect(method = "executeCrafting",at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getCount()I",remap = true))
    private int getCountR(ItemStack instance) {
        int out = instance.getCount();
        if (!this.r$isMEPatternProvider){
            return out;
        } else {
            return out / (int) this.r$craftingFrequency;
        }
    }

    @Redirect(method = "executeCrafting",at = @At(value = "INVOKE", target = "Ljava/util/Map$Entry;getValue()Ljava/lang/Object;",ordinal = 2))
    private Object getValueR(Map.Entry<ICraftingPatternDetails,AccessorTaskProgress> instance) {
        if (r$isMEPatternProvider) {
            //this.remainingOperations -= (int) (this.r$craftingFrequency - 1);
            var value = instance.getValue();
            value.setValue(value.getValue() - (this.r$craftingFrequency - 1));
            return value;
        } else {
            return instance.getValue();
        }
    }

    @Mixin(targets = "appeng.me.cluster.implementations.CraftingCPUCluster$TaskProgress",remap = false)
    public interface AccessorTaskProgress {
        @Accessor
        long getValue();
        @Accessor
        void setValue(long value);
    }
}
