package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.client.gui.GuiListExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiListExtended.class)
public abstract class MixinGuiListExtended {

    @Inject(method = "updateItemPos",at = @At("HEAD"), cancellable = true)
    protected void updateItemPos(int entryID, int insideLeft, int yPos, float partialTicks, final CallbackInfo ci) {
        if (this.getListEntry(entryID) == null){
            ci.cancel();
        }
    }

    @Shadow
    public abstract GuiListExtended.IGuiListEntry getListEntry(int index);
}
