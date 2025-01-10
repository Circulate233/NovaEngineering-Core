package github.kasuminova.novaeng.common.item;

import appeng.api.AEApi;
import appeng.api.features.ILocatable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.features.IWirelessTermRegistry;
import appeng.api.util.AEPartLocation;
import appeng.core.localization.PlayerMessages;
import appeng.core.sync.GuiBridge;
import appeng.items.tools.powered.ToolWirelessTerminal;
import appeng.util.Platform;
import baubles.api.BaublesApi;
import com._0xc4de.ae2exttable.client.gui.AE2ExtendedGUIs;
import com._0xc4de.ae2exttable.client.gui.PartGuiHandler;
import com.glodblock.github.inventory.GuiType;
import com.glodblock.github.util.Util;
import com.mekeng.github.common.container.handler.GuiHandler;
import com.mekeng.github.common.container.handler.MkEGuis;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ItemWirelessUniversalTerminal extends ToolWirelessTerminal {

    public static String NAME = "wireless_universal_terminal";
    IWirelessTermRegistry registry = AEApi.instance().registries().wireless();

    public static int[] getAllMode() {
        List<Integer> modes = new ArrayList<>(Arrays.asList(0, 1, 2, 3));

        if (Loader.isModLoaded("ae2fc")) {
            modes.add(4);
        }
        if (Loader.isModLoaded("mekeng")) {
            modes.add(5);
        }
        if (Loader.isModLoaded("ae2exttable")) {
            modes.addAll(Arrays.asList(6, 7, 8, 9));
        }

        return modes.stream().mapToInt(Integer::intValue).toArray();
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
                    if (stack.getTagCompound().hasKey("Nova")) {
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
                list = Arrays.asList(0);
            }
            int mode = item.getTagCompound().getInteger("mode");
            nbtChange(player, mode, hand);
            switch (mode) {
                case 0:
                    registry.openWirelessTerminalGui(player.getHeldItem(hand), w, player);
                    break;
                case 1, 2, 3:
                    if (list.contains(mode)) {
                        registry.openWirelessTerminalGui(player.getHeldItem(hand), w, player);
                        break;
                    }
                    break;
                case 4:
                    if (Loader.isModLoaded("ae2fc")) {
                        if (list.contains(mode)) {
                            Util.openWirelessTerminal(player.getHeldItem(hand), hand == EnumHand.MAIN_HAND ? player.inventory.currentItem : 40, false, w, player, GuiType.WIRELESS_FLUID_PATTERN_TERMINAL);
                        }
                    }
                    break;
                case 5:
                    if (Loader.isModLoaded("mekeng")) {
                        if (list.contains(mode)) {
                            openWirelessTerminalGui(player.getHeldItem(hand), player, null, mode);
                        }
                    }
                    break;
                case 6,7,8,9:
                    if (Loader.isModLoaded("ae2exttable")) {
                        if (list.contains(mode)) {
                            openWirelessTerminalGui(player.getHeldItem(hand), player, getGui(mode), mode);
                        }
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

    protected void openWirelessTerminalGui(ItemStack item, EntityPlayer player, Object gui, int mode) {
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
                            if (mode == 5) {
                                GuiHandler.openItemGui(player, player.world, player.inventory.currentItem, false, MkEGuis.WIRELESS_GAS_TERM);
                            } else if (mode < 10) {
                                PartGuiHandler.openGUI((AE2ExtendedGUIs) gui, player, new BlockPos(player.inventory.currentItem, 0, Integer.MIN_VALUE), AEPartLocation.fromFacing(EnumFacing.DOWN));
                            }
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
                item.getTagCompound().setInteger("craft", 1);
                if (item.getTagCompound().hasKey("cache")) {
                    NBTTagList cache = item.getTagCompound().getCompoundTag("cache").getTagList(String.valueOf(mode), 10);
                    if (cache.tagCount() != 0) {
                        if (mode < 6) {
                            item.getTagCompound().getCompoundTag("craftingGrid").setTag("Items", cache);
                        } else if (mode < 10) {
                            item.getTagCompound().setTag("crafting", cache);
                        }
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
                item.getTagCompound().setInteger("mode", mode);
                item.getTagCompound().setInteger("craft", 1);
                if (mode != 0 && mode != 2 && mode != 5) {
                    List<Integer> list = Arrays.stream(item.getTagCompound().getIntArray("modes")).boxed().collect(Collectors.toList());
                    if (list.contains(mode)) {
                        NBTTagList cache = item.getTagCompound().getCompoundTag("cache").getTagList(String.valueOf(mode), 10);
                        if (cache.tagCount() != 0) {
                            if (mode < 6) {
                                item.getTagCompound().getCompoundTag("craftingGrid").setTag("Items", cache);
                            } else if (mode < 10){
                                item.getTagCompound().setTag("crafting", cache);
                            }
                            item.getTagCompound().getCompoundTag("cache").removeTag(String.valueOf(mode));
                        }
                    }
                }
            }
        }
        if (Loader.isModLoaded("baubles")) {
            for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                ItemStack item = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
                if (item.getTagCompound() != null && item.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                    item.getTagCompound().setInteger("mode", mode);
                    item.getTagCompound().setInteger("craft", 1);
                    if (mode != 0 && mode != 2 && mode != 5) {
                        List<Integer> list = Arrays.stream(item.getTagCompound().getIntArray("modes")).boxed().collect(Collectors.toList());
                        if (list.contains(mode)) {
                            NBTTagList cache = item.getTagCompound().getCompoundTag("cache").getTagList(String.valueOf(mode), 10);
                            if (cache.tagCount() != 0) {
                                if (mode < 6) {
                                    item.getTagCompound().getCompoundTag("craftingGrid").setTag("Items", cache);
                                } else if (mode < 10){
                                    item.getTagCompound().setTag("crafting", cache);
                                }

                            }
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
                item.getTagCompound().setInteger("craft", 0);
                if (mode != 0 && mode != 2 && mode != 5) {
                    List<Integer> list = Arrays.stream(item.getTagCompound().getIntArray("modes")).boxed().collect(Collectors.toList());
                    if (list.contains(mode)) {
                        item.getTagCompound().setInteger("mode", mode);
                        NBTTagList items = item.getTagCompound().getCompoundTag("craftingGrid").getTagList("Items", 10);
                        if (Loader.isModLoaded("ae2exttable") && item.getTagCompound().hasKey("crafting")) {
                            int ii = 0;
                            int iii = 0;
                            int iiii = 0;
                            switch (mode){case 6: iii = 9;break; case 7: iii = 25;break; case 8: iii = 49;break; case 9: iii = 81;break;}
                            NBTTagList nbtList = new NBTTagList();
                            Iterator<NBTBase> iterator = item.getTagCompound().getTagList("crafting", 10).iterator();
                            while (iterator.hasNext() && ii < iii){
                                NBTBase nbt = iterator.next();
                                if (nbt instanceof NBTTagCompound n){
                                    if (!n.getString("id").endsWith("air") || !n.getString("id").startsWith("minecraft")){
                                        nbtList.appendTag(nbt);
                                        iiii++;
                                    } else {
                                        nbtList.appendTag(new NBTTagCompound());
                                    }
                                }
                                ii++;
                            }
                            if (iiii > 0) {
                                items = nbtList;
                            }
                        }
                        if (items.tagCount() != 0) {
                            if (!item.getTagCompound().hasKey("cache")) {
                                item.getTagCompound().setTag("cache", new NBTTagCompound());
                            }
                            item.getTagCompound().getCompoundTag("cache").setTag(String.valueOf(mode), items);
                        }
                        item.getTagCompound().getCompoundTag("craftingGrid").removeTag("Items");
                        item.getTagCompound().removeTag("crafting");
                    }
                }
            }
        }
        if (Loader.isModLoaded("baubles")) {
            for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                ItemStack item = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
                if (item.getTagCompound() != null && item.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                    int mode = item.getTagCompound().getInteger("mode");
                    item.getTagCompound().setInteger("craft", 0);
                    if (mode != 0 && mode != 2 && mode != 5) {
                        List<Integer> list = Arrays.stream(item.getTagCompound().getIntArray("modes")).boxed().collect(Collectors.toList());
                        if (list.contains(mode)) {
                            if (!item.getTagCompound().hasKey("cache")) {
                                item.getTagCompound().setTag("cache", new NBTTagCompound());
                            }
                            item.getTagCompound().setInteger("mode", mode);
                            NBTTagList items = item.getTagCompound().getCompoundTag("craftingGrid").getTagList("Items", 10);
                            if (Loader.isModLoaded("ae2exttable") && item.getTagCompound().hasKey("crafting")) {
                                int ii = 0;
                                int iii = 0;
                                int iiii = 0;
                                switch (mode){case 6: iii = 9;break; case 7: iii = 25;break; case 8: iii = 49;break; case 9: iii = 81;break;}
                                NBTTagList nbtList = new NBTTagList();
                                Iterator<NBTBase> iterator = item.getTagCompound().getTagList("crafting", 10).iterator();
                                while (iterator.hasNext() && ii < iii){
                                    NBTBase nbt = iterator.next();
                                    if (nbt instanceof NBTTagCompound n){
                                        if (!n.getString("id").endsWith("air") || !n.getString("id").startsWith("minecraft")){
                                            nbtList.appendTag(nbt);
                                            iiii++;
                                        } else {
                                            nbtList.appendTag(new NBTTagCompound());
                                        }
                                    }
                                    ii++;
                                }
                                if (iiii > 0) {
                                    items = nbtList;
                                }
                            }
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
    }

    public static String AllWireless(int value) {
        switch (value) {
            case 1:
                return getString("item.appliedenergistics2.wireless_crafting_terminal.name");
            case 2:
                return getString("item.appliedenergistics2.wireless_fluid_terminal.name");
            case 3:
                return getString("item.appliedenergistics2.wireless_pattern_terminal.name");
            case 4:
                return getString("item.ae2fc:part_fluid_pattern_terminal.name");
            case 5:
                return getString("item.mekeng:wireless_gas_terminal.name");
            case 6:
                return getString("item.ae2exttable.wireless_basic_crafting_terminal.name");
            case 7:
                return getString("item.ae2exttable.wireless_advanced_crafting_terminal.name");
            case 8:
                return getString("item.ae2exttable.wireless_elite_crafting_terminal.name");
            case 9:
                return getString("item.ae2exttable.wireless_ultimate_crafting_terminal.name");
            default:
                return "";
        }
    }

    public static String getString(String value) {
        return "§6(" + I18n.format(value) + ")";
    }

    @Override
    public IGuiHandler getGuiHandler(ItemStack is) {
        if (is.getTagCompound() != null) {
            int mode = is.getTagCompound().getInteger("mode");
            switch (mode) {
                case 0:
                    return GuiBridge.GUI_WIRELESS_TERM;
                case 1:
                    return GuiBridge.GUI_WIRELESS_CRAFTING_TERMINAL;
                case 2:
                    return GuiBridge.GUI_WIRELESS_FLUID_TERMINAL;
                case 3:
                    return GuiBridge.GUI_WIRELESS_PATTERN_TERMINAL;
            }
        }
        return GuiBridge.GUI_WIRELESS_TERM;
    }

    @Override
    protected void getCheckedSubItems(CreativeTabs creativeTab, NonNullList<ItemStack> itemStacks) {
        super.getCheckedSubItems(creativeTab, itemStacks);
        ItemStack charged = new ItemStack(this, 1);
        NBTTagCompound tag = Platform.openNbtData(charged);
        tag.setDouble("internalCurrentPower", this.getAEMaxPower(charged));
        tag.setDouble("internalMaxPower", this.getAEMaxPower(charged));
        tag.setIntArray("modes", getAllMode());
        itemStacks.add(charged);
    }

    @Optional.Method(modid = "ae2exttable")
    public AE2ExtendedGUIs getGuiType(ItemStack item) {
        if (item.getTagCompound() != null) {
            int mode = item.getTagCompound().getInteger("mode");
            return getGui(mode);
        }
        return null;
    }

    @Optional.Method(modid = "ae2exttable")
    public static AE2ExtendedGUIs getGui(int value) {
        switch (value) {
            case 6:
                return AE2ExtendedGUIs.WIRELESS_BASIC_CRAFTING_TERMINAL;
            case 7:
                return AE2ExtendedGUIs.WIRELESS_ADVANCED_CRAFTING_TERMINAL;
            case 8:
                return AE2ExtendedGUIs.WIRELESS_ELITE_CRAFTING_TERMINAL;
            case 9:
                return AE2ExtendedGUIs.WIRELESS_ULTIMATE_CRAFTING_TERMINAL;
        }
        return null;
    }
}
