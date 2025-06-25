package github.kasuminova.novaeng.mixin.fluxnetworks;

import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sonar.fluxnetworks.common.connection.transfer.BasicPointHandler;
import sonar.fluxnetworks.common.connection.transfer.ConnectionTransfer;
import sonar.fluxnetworks.common.connection.transfer.FluxPointHandler;
import sonar.fluxnetworks.common.tileentity.TileFluxPoint;

import java.util.Map;

@Mixin(value = FluxPointHandler.class,remap = false)
public abstract class MixinFluxPointHandler extends BasicPointHandler<TileFluxPoint> {

    @Shadow @Final private Map<EnumFacing, ConnectionTransfer> transfers;

    public MixinFluxPointHandler(TileFluxPoint device) {
        super(device);
    }

    @Redirect(method = "sendToConsumers",at = @At(value = "INVOKE", target = "Lsonar/fluxnetworks/common/connection/transfer/ConnectionTransfer;sendToTile(JZ)J"))
    public long sendToConsumersRedirect(ConnectionTransfer instance, long l, boolean amount) {
        return Math.max(instance.sendToTile(l,amount),0);
    }
}
