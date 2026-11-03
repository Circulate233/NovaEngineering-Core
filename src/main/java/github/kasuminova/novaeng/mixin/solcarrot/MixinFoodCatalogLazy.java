package github.kasuminova.novaeng.mixin.solcarrot;

import com.cazsius.solcarrot.SOLCarrot;
import com.cazsius.solcarrot.client.FoodItemStacks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import squeek.applecore.api.AppleCoreAPI;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Mixin(value = FoodItemStacks.class, remap = false)
public abstract class MixinFoodCatalogLazy {
    @Shadow
    private static List<ItemStack> foodsBeforeBlacklist;
    @Shadow
    private static List<ItemStack> foods;
    @Unique
    private static volatile List<Item> nova$pendingItems;

    @Shadow
    private static Stream<ItemStack> getSubItems(final Item item) {
        throw new AssertionError();
    }

    @Shadow
    private static void applyBlacklist() {
        throw new AssertionError();
    }

    /**
     * @author circulation
     * @reason Capture the registered item roster now; build display stacks only when the catalogue is used.
     */
    @Overwrite
    @SubscribeEvent
    public static synchronized void postInit(final SOLCarrot.PostInitializationEvent event) {
        foodsBeforeBlacklist = null;
        foods = null;
        nova$pendingItems = new ObjectArrayList<>(ForgeRegistries.ITEMS.getValuesCollection());
    }

    @Inject(method = "applyBlacklist", at = @At("HEAD"), cancellable = true, require = 1)
    private static void nova$deferBlacklist(final CallbackInfo ci) {
        if (nova$pendingItems != null && foodsBeforeBlacklist == null) {
            ci.cancel();
        }
    }

    @Inject(method = {"getAllFoods", "getAllFoodsIgnoringBlacklist"}, at = @At("HEAD"), require = 2)
    private static void nova$ensureFoods(final CallbackInfoReturnable<List<ItemStack>> cir) {
        if (nova$pendingItems == null) {
            return;
        }
        synchronized (FoodItemStacks.class) {
            if (nova$pendingItems == null) {
                return;
            }
            final List<ItemStack> result = new ObjectArrayList<>();
            for (final Item item : nova$pendingItems) {
                try (Stream<ItemStack> items = getSubItems(item)) {
                    items.filter(AppleCoreAPI.accessor::isFood).forEachOrdered(result::add);
                }
            }
            final Map<ItemStack, String> names = new Reference2ObjectOpenHashMap<>();
            for (final ItemStack stack : result) {
                names.put(stack, I18n.format(stack.getTranslationKey() + ".name"));
            }
            // Equivalent to the original two stable sorts, with one translation per stack.
            result.sort(Comparator.comparing((ItemStack stack) -> names.get(stack))
                                  .thenComparingInt(ItemStack::getMetadata));
            foodsBeforeBlacklist = result;
            applyBlacklist();
            nova$pendingItems = null;
        }
    }
}
