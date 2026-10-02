package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMPInventoryWork {

    @Unique
    private boolean nova$inventoryChanged;

    @Redirect(method = "sendSlotContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/critereon/InventoryChangeTrigger;trigger(Lnet/minecraft/entity/player/EntityPlayerMP;Lnet/minecraft/entity/player/InventoryPlayer;)V"))
    private void nova$markInventoryChanged(final InventoryChangeTrigger trigger, final EntityPlayerMP player, final InventoryPlayer inventory) {
        this.nova$inventoryChanged = true;
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/PlayerAdvancements;flushDirty(Lnet/minecraft/entity/player/EntityPlayerMP;)V", shift = At.Shift.BEFORE), require = 1)
    private void nova$flushInventoryChanged(final CallbackInfo ci) {
        if (!this.nova$inventoryChanged) {
            return;
        }
        this.nova$inventoryChanged = false;
        final EntityPlayerMP player = (EntityPlayerMP) (Object) this;
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.inventory);
    }
}
