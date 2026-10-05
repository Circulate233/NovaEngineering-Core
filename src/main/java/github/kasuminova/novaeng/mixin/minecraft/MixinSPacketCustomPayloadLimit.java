package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.network.bandwidth.NetworkLimits;
import net.minecraft.network.play.server.SPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SPacketCustomPayload.class)
public class MixinSPacketCustomPayloadLimit {
    @ModifyConstant(method = {"<init>(Ljava/lang/String;Lnet/minecraft/network/PacketBuffer;)V", "readPacketData"},
        constant = @Constant(intValue = 1048576))
    private int novaeng$payloadLimit(int original) {
        return NetworkLimits.payload(true);
    }

    @ModifyConstant(method = {"<init>(Ljava/lang/String;Lnet/minecraft/network/PacketBuffer;)V", "readPacketData"},
        constant = @Constant(stringValue = "Payload may not be larger than 1048576 bytes"))
    private String novaeng$payloadLimitMessage(String original) {
        return "Payload may not be larger than " + NetworkLimits.payload(true) + " bytes";
    }
}
