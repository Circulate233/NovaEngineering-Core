package github.kasuminova.novaeng.common.item;

import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.common.enchantment.MagicBreaking;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ItemBasic extends Item {

    public static List<String> NAMES = Arrays.asList(
            MagicBreaking.MAGICBREAKING.getId() + "_stone"
    );
    protected static Map<String, ItemBasic> map = new Object2ObjectOpenHashMap<>();

    public ItemBasic(final String name) {
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setRegistryName(new ResourceLocation(NovaEngineeringCore.MOD_ID, name));
        this.setTranslationKey(NovaEngineeringCore.MOD_ID + '.' + name);
    }

    public static List<Item> getAllItem() {
        List<Item> ItemBasics = new ObjectArrayList<>();
        for (String name : NAMES) {
            final ItemBasic item = new ItemBasic(name);
            ItemBasics.add(item);
            map.put(name, item);
        }
        return ItemBasics;
    }

    public static ItemBasic getItem(String name) {
        return map.get(name);
    }

    @SideOnly(Side.CLIENT)
    @SuppressWarnings("DataFlowIssue")
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag advancedTooltips) {
        int i = 0;
        while (I18n.hasKey(this.getTranslationKey() + ".tooltip." + i)) {
            lines.add(I18n.format(this.getTranslationKey() + ".tooltip." + i));
            i++;
        }
    }
}
