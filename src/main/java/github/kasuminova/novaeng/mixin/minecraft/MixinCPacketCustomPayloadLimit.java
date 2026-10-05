package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.network.bandwidth.NetworkLimits;
import net.minecraft.network.play.client.CPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(CPacketCustomPayload.class)
public class MixinCPacketCustomPayloadLimit {
    @ModifyConstant(method = {"<init>(Ljava/lang/String;Lnet/minecraft/network/PacketBuffer;)V", "readPacketData"},
        constant = @Constant(intValue = 32767))
    private int novaeng$payloadLimit(int original) {
        return NetworkLimits.payload(false);
    }

    @ModifyConstant(method = {"<init>(Ljava/lang/String;Lnet/minecraft/network/PacketBuffer;)V", "readPacketData"},
        constant = @Constant(stringValue = "Payload may not be larger than 32767 bytes"))
    private String novaeng$payloadLimitMessage(String original) {
        return "Payload may not be larger than " + NetworkLimits.payload(false) + " bytes";
    }
}
