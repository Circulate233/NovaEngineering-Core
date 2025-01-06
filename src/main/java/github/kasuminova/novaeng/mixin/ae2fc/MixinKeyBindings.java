package github.kasuminova.novaeng.mixin.ae2fc;

import baubles.api.BaublesApi;
import com.glodblock.github.FluidCraft;
import com.glodblock.github.client.KeyBindings;
import com.glodblock.github.network.CPacketUseKeybind;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = KeyBindings.class, remap = false)
public class MixinKeyBindings {

    @Final
    @Shadow
    private static KeyBinding WIRELESS_FLUID_PATTERN_TERMINAL;

    /**
     * @author Circulation_
     * @reason 刷新客户端的无限通用终端
     */
    @Overwrite
    @SubscribeEvent
    public static void onKeyInputEvent(InputEvent.KeyInputEvent event) {
        if (WIRELESS_FLUID_PATTERN_TERMINAL.isPressed()) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stackInSlot = player.inventory.getStackInSlot(i);
                if (stackInSlot.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                    RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player,4);
                    break;
                }
            }
            if (Loader.isModLoaded("baubles")) {
                for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                    ItemStack stackInSlot = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
                    if (stackInSlot.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                        RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player, 4);
                        break;
                    }
                }
            }
            FluidCraft.proxy.netHandler.sendToServer(new CPacketUseKeybind());
        }
    }
}
