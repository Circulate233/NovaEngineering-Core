package github.kasuminova.novaeng.client;

import appeng.client.gui.AEBaseGui;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.handler.WirelessTerminalRefresh;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class WirelessUniversalTerminalHandler {

    public static final WirelessUniversalTerminalHandler INSTANCE = new WirelessUniversalTerminalHandler();
    private GuiScreen gui;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (event.getGui() != null){
            if (event.getGui() instanceof AEBaseGui){
                gui = event.getGui();
            } else {
                gui = null;
            }
        } else if (gui != null){
            RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChangeB(player);
            NovaEngineeringCore.NET_CHANNEL.sendToServer(new WirelessTerminalRefresh());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChangeB(event.player);
    }

}
