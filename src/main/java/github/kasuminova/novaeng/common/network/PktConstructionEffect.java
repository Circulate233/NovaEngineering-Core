package github.kasuminova.novaeng.common.network;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload;
import github.kasuminova.novaeng.novaeng_core.Tags;
import io.netty.buffer.ByteBuf;
import io.netty.util.AttributeKey;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** Server-to-client construction animation payload. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PktConstructionEffect implements IMessage, IMessageHandler<PktConstructionEffect, IMessage> {
    private static final String CHANNEL_NAME = "novaeng:build_v3";
    private static final AttributeKey<Boolean> SUPPORTED = AttributeKey.valueOf(CHANNEL_NAME);
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);

    private ConstructionEffectPayload payload;

    public PktConstructionEffect() {
    }

    private PktConstructionEffect(final ConstructionEffectPayload payload) {
        this.payload = payload;
    }

    public static void register() {
        CHANNEL.registerMessage(PktConstructionEffect.class, PktConstructionEffect.class, 0, Side.CLIENT);
    }

    @SubscribeEvent
    public static void onRegistration(final FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if (event.getSide() == Side.SERVER && event.getRegistrations().contains(CHANNEL_NAME)) {
            event.getManager().channel().attr(SUPPORTED).set("REGISTER".equals(event.getOperation()));
        }
    }

    public static boolean supportedBy(final NetworkManager manager) {
        if (manager.isLocalChannel()) return true;
        final var channel = manager.channel();
        return channel != null && channel.hasAttr(SUPPORTED) && Boolean.TRUE.equals(channel.attr(SUPPORTED).get());
    }

    public static void send(final EntityPlayerMP player, final ConstructionEffectPayload payload) {
        CHANNEL.sendTo(new PktConstructionEffect(payload), player);
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        payload = ConstructionEffectPayload.read(buf);
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        ConstructionEffectPayload.write(buf, payload);
    }

    @Override
    public IMessage onMessage(final PktConstructionEffect message, final MessageContext ctx) {
        NovaEngineeringCore.proxy.receiveConstructionEffect(message.payload, ctx.netHandler);
        return null;
    }
}
