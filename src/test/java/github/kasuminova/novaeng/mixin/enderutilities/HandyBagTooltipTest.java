package github.kasuminova.novaeng.mixin.enderutilities;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.enderutilities.gui.client.GuiHandyBag;
import fi.dy.masa.enderutilities.gui.client.base.GuiEnderUtilities;
import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandyBagTooltipTest {
    @Test
    void onlyHandyBagDefersTheParentPassAndOtherGuiArgumentsStayIntact() throws Exception {
        final var handler = MixinGuiEnderUtilitiesTooltip.class.getDeclaredMethod("nova$deferHandyBagTooltip",
            GuiEnderUtilities.class, int.class, int.class, Operation.class);
        handler.setAccessible(true);
        final List<Object[]> calls = new ObjectArrayList<>();
        final Operation<Void> draw = args -> { calls.add(args); return null; };
        final var mixin = new MixinGuiEnderUtilitiesTooltip();
        final GuiHandyBag bag = allocate(GuiHandyBag.class);
        handler.invoke(mixin, bag, 20, 31, draw);
        assertTrue(calls.isEmpty());
        final GuiEnderUtilities ordinary = allocate(GuiEnderUtilities.class);
        handler.invoke(mixin, ordinary, 40, 51, draw);
        assertEquals(1, calls.size());
        assertSame(ordinary, calls.getFirst()[0]);
        assertEquals(40, calls.getFirst()[1]);
        assertEquals(51, calls.getFirst()[2]);
    }

    @Test
    void transformsRealParentWhilePreservingTheHandyBagFinalDrawAfterPotionLayer() throws Exception {
        final String base = "fi.dy.masa.enderutilities.gui.client.base.GuiEnderUtilities";
        final byte[][] bytes = IsolatedMixinTransformer.transformInPackage(
            "github.kasuminova.novaeng.mixin.enderutilities", new String[]{base}, "MixinGuiEnderUtilitiesTooltip");
        final ClassNode transformed = new ClassNode();
        new ClassReader(bytes[0]).accept(transformed, 0);
        int guarded = 0;
        int unguarded = 0;
        for (final var method : transformed.methods) {
            if (!method.name.equals("drawScreen") && !method.name.equals("func_73863_a")) continue;
            for (final var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call) {
                    if (call.name.contains("nova$deferHandyBagTooltip")) guarded++;
                    if (call.name.equals("drawTooltips")) unguarded++;
                }
            }
        }
        assertEquals(1, guarded);
        assertEquals(0, unguarded);
        final ClassNode bag = new ClassNode();
        try (final var input = getClass().getClassLoader().getResourceAsStream(
            "fi/dy/masa/enderutilities/gui/client/GuiHandyBag.class")) {
            new ClassReader(input).accept(bag, 0);
        }
        final List<String> calls = new ObjectArrayList<>();
        for (final var method : bag.methods) {
            if (!method.name.equals("drawScreen") && !method.name.equals("func_73863_a")) continue;
            for (final var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call) calls.add(call.name);
            }
        }
        assertEquals(1, calls.stream().filter("drawTooltips"::equals).count());
        assertTrue(calls.indexOf("drawTooltips") > calls.indexOf("drawActivePotionEffects"));
        assertTrue(calls.indexOf("drawTooltips") > calls.indexOf("updatePositions"));
    }

    private static <T> T allocate(final Class<T> type) throws Exception {
        final Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        final var field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }
}
