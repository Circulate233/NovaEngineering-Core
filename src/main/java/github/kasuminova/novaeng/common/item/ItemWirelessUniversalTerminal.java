package github.kasuminova.novaeng.common.item;

import appeng.api.AEApi;
import appeng.api.features.ILocatable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.features.IWirelessTermRegistry;
import appeng.core.localization.PlayerMessages;
import appeng.core.sync.GuiBridge;
import appeng.items.tools.powered.ToolWirelessTerminal;
import appeng.util.Platform;
import baubles.api.BaublesApi;
import com.glodblock.github.inventory.GuiType;
import com.glodblock.github.util.Util;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemWirelessUniversalTerminal extends ToolWirelessTerminal {

    public static String NAME = "wireless_universal_terminal";
    IWirelessTermRegistry registry = AEApi.instance().registries().wireless();

    public static List<Integer> modes = new ArrayList<>(Arrays.asList(0,1,2,3));

    public static List<Integer> getModes() {
        List<Integer> initial = modes;
        if (Loader.isModLoaded("ae2fc")) {
            initial.add(4);
        }
        return initial;
    }

    public static int[] getAllMode() {
        return getModes().stream().mapToInt(Integer::intValue).toArray();
    }

    public ItemWirelessUniversalTerminal() {
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setRegistryName(new ResourceLocation(NovaEngineeringCore.MOD_ID, NAME));
        this.setTranslationKey(NovaEngineeringCore.MOD_ID + '.' + NAME);
        this.addPropertyOverride(new ResourceLocation("mode"), new IItemPropertyGetter() {
            @SideOnly(Side.CLIENT)
            public float apply(@NotNull ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
                if (stack.getTagCompound() != null) {
                    int mode = stack.getTagCompound().getInteger("mode");
                    if (stack.getTagCompound().hasKey("Nova")){
                        return 114514;
                    } else {
                        return mode;
                    }
                }
                return 0;
            }
        });
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer player, EnumHand hand) {
        ItemStack item = player.getHeldItem(hand);
        if (item.getTagCompound() != null) {
            List<Integer> list;
            if (item.getTagCompound().hasKey("modes")) {
                list = Arrays.stream(item.getTagCompound().getIntArray("modes")).boxed().collect(Collectors.toList());
            } else {
                list = Arrays.asList(-1,0);
            }
            int mode = item.getTagCompound().getInteger("mode");
            nbtChange(player, mode, hand);
            switch (mode) {
                case 0:
                    registry.openWirelessTerminalGui(player.getHeldItem(hand), w, player);
                    break;
                case 1:
                    if (list.contains(mode)) {
                        openWirelessTerminalGui(player.getHeldItem(hand), player, GuiBridge.GUI_WIRELESS_CRAFTING_TERMINAL);
                    }
                    break;
                case 2:
                    if (list.contains(mode)) {
                        openWirelessTerminalGui(player.getHeldItem(hand), player, GuiBridge.GUI_WIRELESS_FLUID_TERMINAL);
                    }
                    break;
                case 3:
                    if (list.contains(mode)) {
                        openWirelessTerminalGui(player.getHeldItem(hand), player, GuiBridge.GUI_WIRELESS_PATTERN_TERMINAL);
                    }
                    break;
                case 4:
                    if (list.contains(mode)) {
                        Util.openWirelessTerminal(player.getHeldItem(hand), hand == EnumHand.MAIN_HAND ? player.inventory.currentItem : 40, false, w, player, GuiType.WIRELESS_FLUID_PATTERN_TERMINAL);
                    }
                    break;
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public boolean canHandle(ItemStack is) {
        return is.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL;
    }

    @Nonnull
    @Override
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        if (stack.getTagCompound() != null) {
            return (I18n.format(this.getUnlocalizedNameInefficiently(stack) + ".name").trim() + AllWireless(stack.getTagCompound().getInteger("mode"))).trim();
        } else {
            return super.getItemStackDisplayName(stack);
        }
    }

    public void openWirelessTerminalGui(ItemStack item, EntityPlayer player, GuiBridge gui) {
        if (!Platform.isClient()) {
            if (!registry.isWirelessTerminal(item)) {
                player.sendMessage(PlayerMessages.DeviceNotWirelessTerminal.get());
            } else {
                IWirelessTermHandler handler = registry.getWirelessTerminalHandler(item);
                String unparsedKey = handler.getEncryptionKey(item);
                if (unparsedKey.isEmpty()) {
                    player.sendMessage(PlayerMessages.DeviceNotLinked.get());
                } else {
                    long parsedKey = Long.parseLong(unparsedKey);
                    ILocatable securityStation = AEApi.instance().registries().locatable().getLocatableBy(parsedKey);
                    if (securityStation == null) {
                        player.sendMessage(PlayerMessages.StationCanNotBeLocated.get());
                    } else {
                        if (handler.hasPower(player, 0.5F, item)) {
                            Platform.openGUI(player, null, null, gui);
                        } else {
                            player.sendMessage(PlayerMessages.DeviceNotPowered.get());
                        }

                    }
                }
            }
        }
    }

    public void nbtChange(EntityPlayer player, int mode, EnumHand hand) {
        if (mode != 0 && mode != 2) {
            ItemStack item = player.getHeldItem(hand);
            if (item.getTagCompound() != null) {
                if (item.getTagCompound().hasKey("cache")){
                    item.getTagCompound().setInteger("craft",1);
                    NBTTagList cache = item.getTagCompound().getCompoundTag("cache").getTagList(String.valueOf(mode), 10);
                    if (cache.tagCount() != 0) {
                        item.getTagCompound().getCompoundTag("craftingGrid").setTag("Items", cache);
                        item.getTagCompound().getCompoundTag("cache").removeTag(String.valueOf(mode));
                    } else {
                        item.getTagCompound().getCompoundTag("cache").removeTag(String.valueOf(mode));
                    }
                }
            }
        }
    }

    public void nbtChange(EntityPlayer player, int mode) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack item = player.inventory.getStackInSlot(i);
            if (item.getTagCompound() != null && item.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                item.getTagCompound().setInteger("mode",mode);
                item.getTagCompound().setInteger("craft",1);
                if (mode != 0 && mode != 2) {
                    NBTTagList cache = item.getTagCompound().getCompoundTag("cache").getTagList(String.valueOf(mode), 10);
                    if (cache.tagCount() != 0) {
                        item.getTagCompound().getCompoundTag("craftingGrid").setTag("Items", cache);
                        item.getTagCompound().getCompoundTag("cache").removeTag(String.valueOf(mode));
                    } else {
                        item.getTagCompound().getCompoundTag("cache").removeTag(String.valueOf(mode));
                    }
                }
            }
        }
        if (Loader.isModLoaded("baubles")) {
            for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                ItemStack item = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
                if (item.getTagCompound() != null && item.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                    item.getTagCompound().setInteger("mode",mode);
                    item.getTagCompound().setInteger("craft",1);
                    if (mode != 0 && mode != 2) {
                        NBTTagList cache = item.getTagCompound().getCompoundTag("cache").getTagList(String.valueOf(mode), 10);
                        if (cache.tagCount() != 0) {
                            item.getTagCompound().getCompoundTag("craftingGrid").setTag("Items", cache);
                            item.getTagCompound().getCompoundTag("cache").removeTag(String.valueOf(mode));
                        } else {
                            item.getTagCompound().getCompoundTag("cache").removeTag(String.valueOf(mode));
                        }
                    }
                }
            }
        }
    }

    public void nbtChangeB(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack item = player.inventory.getStackInSlot(i);
            if (item.getTagCompound() != null && item.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                int mode = item.getTagCompound().getInteger("mode");
                item.getTagCompound().setInteger("craft",0);
                if (mode != 0 && mode != 2) {
                    item.getTagCompound().setInteger("mode", mode);
                    NBTTagList items = item.getTagCompound().getCompoundTag("craftingGrid").getTagList("Items", 10);
                    if (items.tagCount() != 0) {
                        if (!item.getTagCompound().hasKey("cache")){
                            item.getTagCompound().setTag("cache",new NBTTagCompound());
                        }
                        item.getTagCompound().getCompoundTag("cache").setTag(String.valueOf(mode), items);
                        item.getTagCompound().getCompoundTag("craftingGrid").removeTag("Items");
                    } else {
                        item.getTagCompound().getCompoundTag("craftingGrid").removeTag("Items");
                    }
                }
            }
        }
        if (Loader.isModLoaded("baubles")) {
            for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                ItemStack item = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
                if (item.getTagCompound() != null && item.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                    int mode = item.getTagCompound().getInteger("mode");
                    item.getTagCompound().setInteger("craft",0);
                    if (mode != 0 && mode != 2) {
                        if (!item.getTagCompound().hasKey("cache")){
                            item.getTagCompound().setTag("cache",new NBTTagCompound());
                        }
                        item.getTagCompound().setInteger("mode", mode);
                        NBTTagList items = item.getTagCompound().getCompoundTag("craftingGrid").getTagList("Items", 10);
                        if (items.tagCount() != 0) {
                            item.getTagCompound().getCompoundTag("cache").setTag(String.valueOf(mode), items);
                            item.getTagCompound().getCompoundTag("craftingGrid").removeTag("Items");
                        } else {
                            item.getTagCompound().getCompoundTag("craftingGrid").removeTag("Items");
                        }
                    }
                }
            }
        }
    }

    public static String AllWireless(int value) {
        switch (value){
            case 1:
                return "§6(" + I18n.format("item.appliedenergistics2.wireless_crafting_terminal.name") + ")";
            case 2:
                return "§6(" + I18n.format("item.appliedenergistics2.wireless_fluid_terminal.name") + ")";
            case 3:
                return "§6(" + I18n.format("item.appliedenergistics2.wireless_pattern_terminal.name") + ")";
            case 4:
                return "§6(" + I18n.format("item.ae2fc:part_fluid_pattern_terminal.name") + ")";
            default:
                return "";
        }
    }
}