package github.kasuminova.novaeng.mixin.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.util.Annotations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinRemappingTest {
    @Test
    void forgeFluidMinecraftOverrideUsesOneNamedSelectorAndGeneratedMapping() throws Exception {
        final String mixin = "github/kasuminova/novaeng/mixin/geometry/vertex/MixinModelFluidOwnedGeometry";
        final ClassNode type = new ClassNode();
        try (final var input = getClass().getClassLoader().getResourceAsStream(mixin + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(type, 0);
        }
        final var handler = type.methods.stream().filter(method -> method.name.equals("novaeng$beforePublicRead"))
                                        .findFirst().orElseThrow();
        final var injection = Annotations.getVisible(handler, Inject.class);
        assertNotNull(injection);
        assertEquals(List.of("getQuads"), Annotations.<List<String>>getValue(injection, "method"));
        assertTrue(Annotations.<Boolean>getValue(injection, "remap"));

        final var refmap = JsonParser.parseString(Files.readString(Path.of(System.getProperty("novaeng.test.refmap"))))
                                     .getAsJsonObject().getAsJsonObject("mappings").getAsJsonObject(mixin);
        assertNotNull(refmap, "A Forge-owned Minecraft override still needs a generated mapping");
        assertEquals("Lnet/minecraftforge/client/model/ModelFluid$BakedFluid;func_188616_a"
                + "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;J)Ljava/util/List;",
            refmap.get("getQuads").getAsString());
    }
}
