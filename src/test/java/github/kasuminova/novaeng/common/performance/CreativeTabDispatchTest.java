package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.client.util.JeiItemListCollector;
import github.kasuminova.novaeng.testutil.ForgeReflectionEnvironment;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreativeTabDispatchTest {
    @BeforeAll
    static void initializeForgeMappings() {
        ForgeReflectionEnvironment.initialize();
    }

    static class OrdinaryItem extends Item { }

    @Test
    void everyCustomMembershipOrCallbackRetainsFallback() throws Exception {
        final var itemsField = JeiItemListCollector.class.getDeclaredField("DEFAULT_ITEM");
        itemsField.setAccessible(true);
        final ClassValue<?> items = (ClassValue<?>) itemsField.get(null);
        assertEquals(true, items.get(Item.class));
        assertEquals(true, items.get(OrdinaryItem.class));
        assertEquals(true, items.get(OrdinaryGrandchild.class));
        assertEquals(true, items.get(OverloadedCallbacks.class));
        for (final Class<?> type : new Class<?>[]{CustomSubtypes.class, CustomMembership.class, InheritedCustomMembership.class,
            CustomTabs.class, CustomPrimaryTab.class}) {
            assertEquals(false, items.get(type), type.getName());
        }
        final var tabsField = JeiItemListCollector.class.getDeclaredField("DEFAULT_TAB");
        tabsField.setAccessible(true);
        final ClassValue<?> tabs = (ClassValue<?>) tabsField.get(null);
        assertEquals(true, tabs.get(OrdinaryTab.class));
        assertEquals(false, tabs.get(CustomTab.class));
        assertEquals(false, tabs.get(InheritedCustomTab.class));
    }

    static class OrdinaryGrandchild extends OrdinaryItem {
    }
    static class CustomSubtypes extends Item {
        @Override public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) { }
    }
    static class CustomMembership extends Item {
        @Override protected boolean isInCreativeTab(CreativeTabs tab) { return true; }
    }

    static class OverloadedCallbacks extends Item {
        public void getSubItems(NonNullList<ItemStack> items) {
        }

        protected boolean isInCreativeTab() {
            return true;
        }

        public CreativeTabs getCreativeTab(int index) {
            return null;
        }
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

    static class InheritedCustomMembership extends CustomMembership {
    }

    abstract static class InheritedCustomTab extends CustomTab {
    }
}
