package github.kasuminova.novaeng.mixin.zenutils;

import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.mc1120.item.MCItemStack;
import github.kasuminova.novaeng.testutil.ForgeReflectionEnvironment;
import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTooltipMatchingTest {
    @BeforeAll
    static void initializeMinecraft() {
        ForgeReflectionEnvironment.initialize();
        Bootstrap.register();
    }

    private static Method handler() throws Exception {
        final var method = MixinItemTooltipMatching.class.getDeclaredMethod("nova$skipUnrelatedTooltipWrapper",
            ItemStack.class, Map.Entry.class);
        method.setAccessible(true);
        return method;
    }

    private static IItemStack wrap(final IIngredient selector, final ItemStack stack) throws Exception {
        return (IItemStack) handler().invoke(null, stack,
            new AbstractMap.SimpleImmutableEntry<>(selector, null));
    }

    @Test
    void mismatchedPlainItemKeepsVanillaMatchResultWithoutDeepNbtConversion() throws Exception {
        final Item selected = new Item();
        final Item other = new Item();
        for (int size : new int[]{1, 2, 64}) {
            for (int metadata : new int[]{0, 5, 32767}) {
                final IIngredient selector = new MCItemStack(new ItemStack(selected, size, metadata));
                final ItemStack candidate = new ItemStack(other, size, metadata);
                final NBTTagCompound tag = new NBTTagCompound();
                final CountingTag child = new CountingTag();
                child.setIntArray("payload", new int[4096]);
                tag.setTag("nested", child);
                candidate.setTagCompound(tag);
                final IItemStack baseline = CraftTweakerMC.getIItemStackForMatching(candidate);
                assertTrue(child.keyReads > 0, "baseline must traverse the nested NBT");
                child.keyReads = 0;
                final IItemStack optimized = wrap(selector, candidate);
                assertEquals(0, child.keyReads, "unrelated items must skip deep NBT conversion");
                assertNull(optimized);
                assertFalse(selector.matches(baseline));
                assertEquals(selector.matches(baseline), selector.matches(optimized));
            }
        }
    }

    @Test
    void matchingItemsTaggedSelectorsAndCustomMatchersAlwaysGetFreshCurrentWrapper() throws Exception {
        final Item one = new Item();
        final Item two = new Item();
        final ItemStack candidate = new ItemStack(one);
        final IIngredient same = new MCItemStack(new ItemStack(one));
        final IItemStack first = wrap(same, candidate);
        candidate.setCount(3);
        final NBTTagCompound changed = new NBTTagCompound();
        changed.setInteger("changed", 19);
        candidate.setTagCompound(changed);
        final IItemStack second = wrap(same, candidate);
        assertNotSame(first, second);
        assertEquals(3, second.getAmount());
        assertEquals(19, second.getTag().memberGet("changed").asInt());

        final ItemStack tagged = new ItemStack(two);
        tagged.setTagCompound(changed.copy());
        final IItemStack taggedWrapper = wrap(new MCItemStack(tagged), candidate);
        assertNotSame(second, taggedWrapper);
        assertEquals(19, taggedWrapper.getTag().memberGet("changed").asInt());
        final IIngredient subclass = new MCItemStack(new ItemStack(two)) {
            @Override public boolean matches(final IItemStack item) { return item != null; }
        };
        final IItemStack subclassWrapper = wrap(subclass, candidate);
        assertNotSame(taggedWrapper, subclassWrapper);
        assertTrue(subclass.matches(subclassWrapper));
        final IIngredient custom = (IIngredient) Proxy.newProxyInstance(IIngredient.class.getClassLoader(),
            new Class<?>[]{IIngredient.class}, (proxy, method, args) -> {
                if (method.getName().equals("matches")) return args[0] != null;
                throw new AssertionError("Custom ingredient must not be inspected: " + method);
            });
        final IItemStack customWrapper = wrap(custom, candidate);
        assertNotSame(subclassWrapper, customWrapper);
        assertTrue(custom.matches(customWrapper));
    }

    @Test
    void realZenUtilsLoopTransformsButKeepsMatchesAndModifierCallbacks() throws Exception {
        final byte[][] bytes = IsolatedMixinTransformer.transformInPackage(
            "github.kasuminova.novaeng.mixin.zenutils",
            new String[]{"youyihj.zenutils.api.item.ItemTooltipModification$TooltipEventHandler"},
            "MixinItemTooltipMatching");
        final ClassNode target = new ClassNode();
        new ClassReader(bytes[0]).accept(target, 0);
        int guarded = 0;
        int matches = 0;
        int modifiers = 0;
        int unguarded = 0;
        for (final var method : target.methods) {
            if (!method.name.equals("onItemTooltip")) continue;
            for (final var instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                if (call.name.contains("nova$skipUnrelatedTooltipWrapper")) guarded++;
                if (call.name.equals("getIItemStackForMatching")) unguarded++;
                if (call.owner.equals("crafttweaker/api/item/IIngredient") && call.name.equals("matches")) matches++;
                if (call.owner.equals("youyihj/zenutils/api/item/ITooltipModifier") && call.name.equals("apply")) modifiers++;
            }
        }
        assertEquals(1, guarded);
        assertEquals(0, unguarded);
        assertEquals(1, matches);
        assertEquals(1, modifiers);
    }

    private static final class CountingTag extends NBTTagCompound {
        private int keyReads;

        @Override
        public Set<String> getKeySet() {
            keyReads++;
            return super.getKeySet();
        }
    }
}
