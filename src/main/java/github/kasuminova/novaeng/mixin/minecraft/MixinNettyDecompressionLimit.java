package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.network.bandwidth.NetworkLimits;
import net.minecraft.network.NettyCompressionDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(NettyCompressionDecoder.class)
public class MixinNettyDecompressionLimit {
    @ModifyConstant(method = "decode", constant = @Constant(intValue = 2097152))
    private int novaeng$decompressedLimit(int original) {
        return NetworkLimits.decompressed();
    }
}
