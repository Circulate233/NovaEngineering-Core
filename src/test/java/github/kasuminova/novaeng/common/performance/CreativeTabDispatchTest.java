package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.client.util.JeiItemListCollector;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreativeTabDispatchTest {
    static class OrdinaryItem extends Item { }
    static class CustomSubtypes extends Item {
        @Override public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) { }
    }
    static class CustomMembership extends Item {
        @Override protected boolean isInCreativeTab(CreativeTabs tab) { return true; }
    }
    static class CustomTabs extends Item {
        @Override public CreativeTabs[] getCreativeTabs() { return new CreativeTabs[0]; }
    }
    static class CustomPrimaryTab extends Item {
        @Override public CreativeTabs getCreativeTab() { return null; }
    }
    abstract static class OrdinaryTab extends CreativeTabs {
        OrdinaryTab() { super("test"); }
    }
    abstract static class CustomTab extends OrdinaryTab {
        @Override public void displayAllRelevantItems(NonNullList<ItemStack> items) { }
    }

    @Test
    void everyCustomMembershipOrCallbackRetainsFallback() throws Exception {
        final var itemsField = JeiItemListCollector.class.getDeclaredField("DEFAULT_ITEM");
        itemsField.setAccessible(true);
        final ClassValue<?> items = (ClassValue<?>) itemsField.get(null);
        assertEquals(true, items.get(Item.class));
        assertEquals(true, items.get(OrdinaryItem.class));
        for (final Class<?> type : new Class<?>[]{CustomSubtypes.class, CustomMembership.class, CustomTabs.class, CustomPrimaryTab.class}) {
            assertEquals(false, items.get(type), type.getName());
        }
        final var tabsField = JeiItemListCollector.class.getDeclaredField("DEFAULT_TAB");
        tabsField.setAccessible(true);
        final ClassValue<?> tabs = (ClassValue<?>) tabsField.get(null);
        assertEquals(true, tabs.get(OrdinaryTab.class));
        assertEquals(false, tabs.get(CustomTab.class));
    }
}
