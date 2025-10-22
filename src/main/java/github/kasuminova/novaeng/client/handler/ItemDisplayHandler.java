package github.kasuminova.novaeng.client.handler;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ItemDisplayHandler {

//    @SubscribeEvent(priority = EventPriority.HIGHEST)
//    public void onInputEvent(GuiScreenEvent.KeyboardInputEvent.Pre event) {
//        var mc = Minecraft.getMinecraft();
//        if (mc.currentScreen instanceof GuiContainer gui) {
//            int eventKey = Keyboard.getEventKey();
//            if (GuiScreen.isCtrlKeyDown() && Keyboard.KEY_L == eventKey && Keyboard.isKeyDown(eventKey)) {
//                var slot = gui.getSlotUnderMouse();
//                if (slot != null) {
//                    NovaEngineeringCore.NET_CHANNEL.sendToServer(new ItemDisplayPkt(slot.getStack(), mc.player));
//                }
//            }
//        }
//    }

}