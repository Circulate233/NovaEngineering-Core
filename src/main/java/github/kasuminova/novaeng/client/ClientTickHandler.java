package github.kasuminova.novaeng.client;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.item.ItemWirelessUniversalTerminal;
import github.kasuminova.novaeng.common.network.UpdateItemModeMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;


@SideOnly(Side.CLIENT)
public class ClientTickHandler {
    public static final ClientTickHandler INSTANCE = new ClientTickHandler();
    public static Minecraft mc = FMLClientHandler.instance().getClient();

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        if (mc.player != null && mc.player.isSneaking()) {
            ItemStack stack = mc.player.getHeldItemMainhand();
            int delta = -Mouse.getEventDWheel();
            if (delta % 120 == 0){
                delta = delta / 120 ;
            }
            if (stack.getItem() instanceof ItemWirelessUniversalTerminal && delta != 0) {
                int newVal = stack.getTagCompound().getInteger("mode") + (delta % 5);

                if (newVal > 0) {
                    newVal = newVal % 5;
                } else if (newVal < 0) {
                    newVal = 5 + newVal;
                }

                stack.getTagCompound().setInteger("mode",newVal);
                NovaEngineeringCore.NET_CHANNEL.sendToServer(new UpdateItemModeMessage(stack,newVal));

                event.setCanceled(true);
            }
        }
    }

}
