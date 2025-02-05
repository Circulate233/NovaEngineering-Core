package github.kasuminova.novaeng.client.handler;

import appeng.client.gui.AEBaseGui;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.handler.WirelessTerminalRefresh;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WirelessUniversalTerminalHandler {

    public static final WirelessUniversalTerminalHandler INSTANCE = new WirelessUniversalTerminalHandler();
    private GuiScreen gui;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() != null){
            if (event.getGui() instanceof AEBaseGui){
                gui = event.getGui();
            } else {
                gui = null;
            }
        } else if (gui != null){
            NovaEngineeringCore.NET_CHANNEL.sendToServer(new WirelessTerminalRefresh());
        }
    }

}
