package github.kasuminova.novaeng.mixin.ae2;

import appeng.core.sync.AppEngPacket;
import appeng.core.sync.AppEngPacketHandlerBase;
import appeng.core.sync.network.AppEngClientPacketHandler;
import github.kasuminova.novaeng.common.profiler.CPacketProfiler;
import io.netty.buffer.ByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.InvocationTargetException;

@Mixin(AppEngClientPacketHandler.class)
public class MixinAppEngClientPacketHandler {

    @SuppressWarnings("MethodMayBeStatic")
    @Redirect(
            method = "onPacketData",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/core/sync/AppEngPacketHandlerBase$PacketTypes;parsePacket(Lio/netty/buffer/ByteBuf;)Lappeng/core/sync/AppEngPacket;",
                    remap = false
            ),
            remap = false
    )
    private AppEngPacket redirectParsePacket(final AppEngPacketHandlerBase.PacketTypes instance, 
                                             final ByteBuf in) throws InvocationTargetException, InstantiationException, IllegalAccessException
    {
        int prevIndex = in.readerIndex();
        AppEngPacket packet = instance.parsePacket(in);

        CPacketProfiler.onPacketReceived(packet, in.readerIndex() - prevIndex);

        return packet;
    }

}
