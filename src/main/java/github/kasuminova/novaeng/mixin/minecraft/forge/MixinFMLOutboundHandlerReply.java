package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.collect.ImmutableList;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.NovaEngineeringCore;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Forge keeps the send target of a packet in a channel attribute ({@code FML_MESSAGETARGET}) rather than on the packet
 * itself, so the target a packet is dispatched with is whatever that attribute holds when the outbound handler reads
 * it, which is not necessarily what the caller set before writing. The cleanest way to watch the two come apart is
 * {@code OutboundTarget.REPLY} - the third constant with a class body, hence {@code $3} - which answers
 * {@code ImmutableList.of(packet.getDispatcher())} for "send this back to where it came from". A packet built by the
 * outbound codec has no originator dispatcher, so under this target there is nothing it could be delivered to; unlike
 * PLAYER and TOSERVER, which guard the same null earlier in this very enum, REPLY does not check and throws. That
 * NullPointerException is caught by {@code FMLProxyPacket.processPacket}, whose catch answers
 * {@code rejectHandshake("A fatal error has occurred, this connection is terminated")}, so the price of the race is a
 * disconnect rather than a log line.
 * <p>
 * The case measured in this pack is an Astral Sorcery ritual: {@code CEffectHorologium.playEffect} broadcasts to
 * everyone within 16 blocks of its pedestal, and when a reply is written on the same channel in between, the broadcast
 * is dispatched as REPLY because the outbound handler reads the attribute the reply left behind. Such a packet cannot
 * be delivered under that target - there is no dispatcher to reply to - so drop it instead of throwing. The effect
 * misses one client update it was never going to send, and the player stays in the world. Nothing but the null is
 * guarded; every other packet still leaves through the original call.
 * <p>
 * This is anchored to an anonymous enum constant body, whose {@code $3} suffix is only its position in the enum's
 * declaration order. The identity check against {@code OutboundTarget.REPLY} in the handler keeps that from mattering:
 * were a later Forge to renumber the constants, the handler would find itself in a constant that never asked for the
 * guard and answer with the original call, so the cost of that drift is the exception coming back, not a guard placed
 * on the wrong target.
 */
@Mixin(targets = "net.minecraftforge.fml.common.network.FMLOutboundHandler$OutboundTarget$3", remap = false)
public abstract class MixinFMLOutboundHandlerReply {

    @Unique
    private static boolean nova$reportedMissingOriginator;

    /**
     * The originator is declared as an {@code Object} because that is the erased type of the argument at this call
     * site: {@code FMLProxyPacket#getDispatcher} hands its {@code NetworkDispatcher} to {@code ImmutableList.of(E)},
     * whose descriptor is {@code of(Object)}, and Mixin matches this handler against that descriptor. Declaring the
     * dispatcher type here makes the mixin refuse to apply.
     */
    @Redirect(method = "selectNetworks", at = @At(value = "INVOKE",
            target = "Lcom/google/common/collect/ImmutableList;of(Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;"),
            remap = false, require = 1)
    private ImmutableList<NetworkDispatcher> nova$dropReplyWithoutOriginator(final Object originator) {
        if (originator != null || !NovaEngCoreConfig.SERVER.guardForgeNetworkReplyTarget
                || (Object) this != FMLOutboundHandler.OutboundTarget.REPLY) {
            return ImmutableList.of((NetworkDispatcher) originator);
        }
        if (!nova$reportedMissingOriginator) {
            nova$reportedMissingOriginator = true;
            NovaEngineeringCore.log.warn("A packet was dispatched with Forge's REPLY target while carrying no" +
                    " originator dispatcher, the one combination OutboundTarget.REPLY throws a NullPointerException" +
                    " on. FMLProxyPacket.processPacket turns that exception into a rejected handshake (\"A fatal error" +
                    " has occurred, this connection is terminated\"), which drops the player out of the world. Forge" +
                    " keeps the send target in a channel attribute, so a reply written on the same channel can take" +
                    " it away from a send that is still in flight; this happened to an Astral Sorcery ritual" +
                    " broadcasting to everyone around its pedestal. The packet is dropped, which is all the reply" +
                    " target could have done with it. Set GuardForgeNetworkReplyTarget=false to get the exception" +
                    " back.");
        }
        return ImmutableList.of();
    }
}
