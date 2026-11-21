package github.kasuminova.novaeng.mixin.mekanism;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolarActivatorTexturePreloadTest {
    @Test
    void declaredDependencyContainsExactlyOneMatchingPreload() throws Exception {
        final ClassNode target = new ClassNode();
        try (var input = getClass().getResourceAsStream(
            "/mekanism/multiblockmachine/client/MultiblockMachineClientProxy.class")) {
            assertNotNull(input, "The actual Mekanism dependency must be checked");
            new ClassReader(input).accept(target, 0);
        }
        int calls = 0;
        for (final var method : target.methods) {
            if (!method.name.equals("registerTESRs") || !method.desc.equals("()V")) continue;
            for (final var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("mekanism/client/render/MekanismRenderer")
                    && call.name.equals("bindTexture")
                    && call.desc.equals("(Lnet/minecraft/util/ResourceLocation;)V")) calls++;
            }
        }
        assertEquals(1, calls, "The supported version must execute this optimization");
    }

    @Test
    void versionsWithoutTheScreenPreloadCanStartEvenWithInjectionDebugging() throws Exception {
        final var method = MixinSolarActivatorTexturePreload.class.getDeclaredMethod(
            "nova$keepOtherPreloads", ResourceLocation.class);
        final var injection = method.getAnnotation(WrapWithCondition.class);
        assertEquals(0, injection.require());
        assertEquals(0, injection.expect());
    }

    @Test
    void onlyTheSolarActivatorScreenPreloadIsDeferred() throws Exception {
        final var method = MixinSolarActivatorTexturePreload.class.getDeclaredMethod(
            "nova$keepOtherPreloads", ResourceLocation.class);
        method.setAccessible(true);
        final var mixin = new MixinSolarActivatorTexturePreload() {
        };
        final String screen = "render/machine/largesolarneutronactivator/screen.png";
        assertFalse((Boolean) method.invoke(mixin, new ResourceLocation("mekanismmultiblockmachine", screen)));
        assertTrue((Boolean) method.invoke(mixin, new ResourceLocation("othermod", screen)));
        assertTrue((Boolean) method.invoke(mixin, new ResourceLocation("mekanismmultiblockmachine", "render/other.png")));
    }
}
