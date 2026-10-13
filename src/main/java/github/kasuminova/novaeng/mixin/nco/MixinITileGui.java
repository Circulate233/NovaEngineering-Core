package github.kasuminova.novaeng.mixin.nco;

import nc.tile.ITileGui;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = ITileGui.class, remap = false)
public interface MixinITileGui {

    @Shadow
    Set<EntityPlayer> getTileUpdatePacketListeners();

    @Inject(method = "sendTileUpdatePacketToListeners", at = @At("HEAD"), cancellable = true, remap = false)
    default void nova$skipPacketWithoutListeners(final CallbackInfo ci) {
        if (this.getTileUpdatePacketListeners().isEmpty()) {
            ci.cancel();
        }
    }
}
