package github.kasuminova.novaeng.mixin.ae2;

import appeng.client.ClientHelper;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketTerminalUse;
import appeng.items.tools.powered.Terminal;
import appeng.server.ServerHelper;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import static appeng.client.KeyBindings.*;

@Mixin(value = ClientHelper.class,remap = false)
public class MixinClientHelper extends ServerHelper {

    @Final
    @Shadow
    private List<KeyBinding> keyBindings;

    /**
     * @author Circulation_
     * @reason 使无线通用终端使用ae2快捷键打开时刷新nbt以防止打开错误的模式
     */
    @Overwrite
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInputEvent(final InputEvent.KeyInputEvent event) {
        for (KeyBinding k : keyBindings) {
            if (k.isPressed()) {
                EntityPlayerSP player = Minecraft.getMinecraft().player;
                if (k == WT.getKeyBinding()) {
                    RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player,0);
                    NetworkHandler.instance().sendToServer(new PacketTerminalUse(Terminal.WIRELESS_TERMINAL));
                } else if (k == WCT.getKeyBinding()) {
                    RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player,1);
                    NetworkHandler.instance().sendToServer(new PacketTerminalUse(Terminal.WIRELESS_CRAFTING_TERMINAL));
                } else if (k == WPT.getKeyBinding()) {
                    RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player,3);
                    NetworkHandler.instance().sendToServer(new PacketTerminalUse(Terminal.WIRELESS_PATTERN_TERMINAL));
                } else if (k == WFT.getKeyBinding()) {
                    RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player,2);
                    NetworkHandler.instance().sendToServer(new PacketTerminalUse(Terminal.WIRELESS_FLUID_TERMINAL));
                }
            }
        }
    }
}
