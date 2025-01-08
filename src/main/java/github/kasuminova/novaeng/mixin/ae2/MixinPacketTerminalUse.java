package github.kasuminova.novaeng.mixin.ae2;

import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketTerminalUse;
import appeng.items.tools.powered.Terminal;
import baubles.api.BaublesApi;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(value = PacketTerminalUse.class,remap = false)
public class MixinPacketTerminalUse extends AppEngPacket {

    @Shadow
    Terminal terminal;
    @Shadow
    void openGui(ItemStack itemStack, int slotIdx, EntityPlayer player, boolean isBauble){}

    /**
     * @author Circulation_
     * @reason 使无线通用终端可以使用ae2快捷键
     */
    @Overwrite
    public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player) {
        NonNullList<ItemStack> mainInventory = player.inventory.mainInventory;

        for(int i = 0; i < mainInventory.size(); ++i) {
            ItemStack is = mainInventory.get(i);

            if (is.getTagCompound() != null) {
                int mode = novaEngineering_Core$determineMode(terminal.name());

                List<Integer> list = null;
                if (is.getTagCompound().hasKey("modes")) {
                    list = Arrays.stream(is.getTagCompound().getIntArray("modes")).boxed().collect(Collectors.toList());
                }

                if (terminal.getItemDefinition().isSameAs(is)) {
                    openGui(is, i, player, false);
                    return;
                } else if (is.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL && list != null && list.contains(mode)) {
                    RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player, mode);
                    openGui(is, i, player, false);
                    return;
                }
            }
        }

        if (Loader.isModLoaded("baubles")) {
            tryOpenBauble(player);
        }

    }

    /**
     * @author Circulation_
     * @reason 同上,使得额外支持饰品栏
     */
    @Overwrite
    @Optional.Method(modid = "baubles")
    void tryOpenBauble(EntityPlayer player) {
        for(int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); ++i) {
            ItemStack is = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (is.getTagCompound() != null) {
                int mode = novaEngineering_Core$determineMode(terminal.name());
                List<Integer> list = null;
                if (is.getTagCompound().hasKey("modes")) {
                    list = Arrays.stream(is.getTagCompound().getIntArray("modes")).boxed().collect(Collectors.toList());
                }
                if (terminal.getItemDefinition().isSameAs(is)) {
                    openGui(is, i, player, true);
                    break;
                } else if (is.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL && list != null && list.contains(mode)) {
                    RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player, mode);
                    openGui(is, i, player, true);
                    return;
                }
            }
        }
    }

    @Unique
    private int novaEngineering_Core$determineMode(String value) {
        switch (value){
            case "WIRELESS_CRAFTING_TERMINAL" :
                return 1;
            case "WIRELESS_PATTERN_TERMINAL":
                return 3;
            case "WIRELESS_FLUID_TERMINAL" :
                return 2;
            default:return 0;
        }
    }
}
