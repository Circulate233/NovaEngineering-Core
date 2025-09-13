package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.DualityInterface;
import com.circulation.random_complement.client.RCSettings;
import com.circulation.random_complement.client.buttonsetting.IntelligentBlocking;
import com.circulation.random_complement.common.interfaces.RCIConfigurableObject;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.inventory.InventoryCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DualityInterface.class,remap = false)
public abstract class MixinDualityInterface implements RCIConfigurableObject {

    @Shadow
    protected abstract boolean isBlocking();

    @Unique
    private int r$lastInputHash;

    @Inject(method = "isBusy", at = @At(value = "INVOKE", target = "Lappeng/helpers/DualityInterface;isBlocking()Z", shift = At.Shift.AFTER), cancellable = true)
    public void isIntelligentBlocking(CallbackInfoReturnable<Boolean> cir) {
        if (this.r$getConfigManager().getSetting(RCSettings.IntelligentBlocking) == IntelligentBlocking.OPEN) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pushPattern", at = @At("RETURN"))
    public void intelligentBlocking(ICraftingPatternDetails pattern, InventoryCrafting table, CallbackInfoReturnable<Boolean> cir) {
        if (this.r$getConfigManager().getSetting(RCSettings.IntelligentBlocking) == IntelligentBlocking.OPEN && cir.getReturnValue()) {
            this.r$lastInputHash = pattern.hashCode();
        }
    }

    @Redirect(method = "pushPattern", at = @At(value = "INVOKE", target = "Lappeng/helpers/DualityInterface;isBlocking()Z"))
    public boolean intelligentBlocking(DualityInterface instance, @Local(name = "patternDetails") ICraftingPatternDetails patternDetails) {
        boolean b = this.isBlocking();
        if (b) {
            return this.r$getConfigManager()
                    .getSetting(RCSettings.IntelligentBlocking) != IntelligentBlocking.OPEN
                    || this.r$lastInputHash != patternDetails.hashCode();
        }
        return b;
    }

}