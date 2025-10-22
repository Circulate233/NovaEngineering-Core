package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketRecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    /**
     * @author circulation
     * @reason 废弃服务端到客户端的配方书同步
     */
    @Overwrite
    public void handleRecipeBook(SPacketRecipeBook packetIn) {
    }
}
