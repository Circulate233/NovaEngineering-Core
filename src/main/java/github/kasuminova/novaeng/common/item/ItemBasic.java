package github.kasuminova.novaeng.common.item;

import github.kasuminova.novaeng.common.core.CreativeTabNovaEng;
import github.kasuminova.novaeng.common.enchantment.MagicBreaking;
import github.kasuminova.novaeng.common.util.Functions;
import github.kasuminova.novaeng.novaeng_core.Tags;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class ItemBasic extends Item {

    protected static final Map<String, ItemBasic> ITEMS = new Object2ObjectOpenHashMap<>();
    private static final List<Declared> DECLARED = new ObjectArrayList<>();
    public static List<String> NAMES = Functions.asList(MagicBreaking.MAGICBREAKING.getId() + "_stone");
    private static List<Item> cachedItems;
    private final boolean glint;
    private final boolean beaconPayment;

    public ItemBasic(final String name) {
        this(name, 1);
    }

    public ItemBasic(final String name, final int maxStack) {
        this(name, maxStack, false);
    }

    public ItemBasic(final String name, final int maxStack, final boolean glint) {
        this(name, maxStack, glint, false);
    }

    public ItemBasic(final String name, final int maxStack, final boolean glint, final boolean beaconPayment) {
        this.glint = glint;
        this.beaconPayment = beaconPayment;
        this.setMaxStackSize(maxStack);
        this.setCreativeTab(CreativeTabNovaEng.INSTANCE);
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, name));
        this.setTranslationKey(Tags.MOD_ID + '.' + name);
    }

    public static void declare(final String name) {
        declare(name, 64);
    }

    public static void declare(final String name, final int maxStack) {
        declare(name, maxStack, false);
    }

    public static void declare(final String name, final int maxStack, final boolean glint) {
        declare(name, maxStack, glint, false);
    }

    public static void declare(final String name, final int maxStack, final boolean glint, final boolean beaconPayment) {
        DECLARED.add(new Declared(name, maxStack, glint, beaconPayment));
    }

    public static List<Item> getAllItem() {
        if (cachedItems == null) {
            final List<Item> items = new ObjectArrayList<>(NAMES.size() + DECLARED.size());
            for (final String name : NAMES) {
                final ItemBasic item = new ItemBasic(name);
                items.add(item);
                ITEMS.put(name, item);
            }
            for (final Declared spec : DECLARED) {
                final ItemBasic item = new ItemBasic(spec.name(), spec.maxStack(), spec.glint(), spec.beaconPayment());
                items.add(item);
                ITEMS.put(spec.name(), item);
            }
            cachedItems = items;
        }
        return cachedItems;
    }

    @Nullable
    public static ItemBasic getItem(@Nullable final String name) {
        getAllItem();
        return ITEMS.get(name);
    }

    @Override
    public boolean hasEffect(final ItemStack stack) {
        return this.glint;
    }

    @Override
    public boolean isBeaconPayment(final ItemStack stack) {
        return this.beaconPayment;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<String> lines, final ITooltipFlag flag) {
        for (int i = 0; I18n.hasKey(this.getTranslationKey() + ".tooltip." + i); i++) {
            lines.add(I18n.format(this.getTranslationKey() + ".tooltip." + i));
        }
    }

    private record Declared(String name, int maxStack, boolean glint, boolean beaconPayment) {
    }
}
