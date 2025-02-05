package github.kasuminova.novaeng.common.handler;

import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class WirelessUniversalTerminalHandler {

    public static final WirelessUniversalTerminalHandler INSTANCE = new WirelessUniversalTerminalHandler();

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChangeB(event.player);
    }

}
