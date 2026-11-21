package github.kasuminova.novaeng.mixin.actinium;

import com.google.common.collect.ImmutableMap;
import github.kasuminova.novaeng.client.geometry.item.CompactItemModel;
import github.kasuminova.novaeng.client.geometry.item.ItemContour;
import github.kasuminova.novaeng.mixin.geometry.actinium.MixinGeometryDisplayLists;
import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryDisplayListAllocationTest {
    private static final String TARGET = "com/dhj/actinium/render/FastLitItemDisplayListCache";

    @BeforeAll
    static void bindConstantsForUnmergedPredicateTests() throws Exception {
        // Direct unit calls use the unmerged mixin; bind its shadow to the actual dependency's
        // constant without initializing Actinium or constructing its render caches.
        try (final var input = GeometryDisplayListAllocationTest.class.getClassLoader()
            .getResourceAsStream(TARGET + ".class")) {
            assertNotNull(input);
            final ClassNode target = new ClassNode();
            new ClassReader(input).accept(target, ClassReader.SKIP_CODE);
            final var constant = target.fields.stream()
                .filter(field -> field.name.equals("VANILLA_MODEL_WRAPPER_BAKED")).findFirst().orElseThrow();
            assertTrue(constant.value instanceof String);
            final var shadow = MixinGeometryDisplayLists.class.getDeclaredField(constant.name);
            shadow.setAccessible(true);
            shadow.set(null, constant.value);
        }
    }

    private static long originalCalls(final MethodNode method, final String name) {
        long calls = 0;
        for (final var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(TARGET) && call.name.equals(name))
                calls++;
        }
        return calls;
    }

    @Test
    void onlyUnexposedItemFormatGeometryIsStable() {
        final CompactItemModel item = new CompactItemModel(new ItemContour[0], new TextureAtlasSprite[0],
            null, ImmutableMap.of(), ItemOverrideList.NONE);
        assertTrue(MixinGeometryDisplayLists.isStableModel(item));
        item.getQuads(null, null, 0);
        assertTrue(item.novaeng$isGeometryExposed());
        assertFalse(MixinGeometryDisplayLists.isStableModel(item));

        final CompactItemModel blockFormat = new CompactItemModel(new ItemContour[0], new TextureAtlasSprite[0],
            null, ImmutableMap.of(), ItemOverrideList.NONE, DefaultVertexFormats.BLOCK);
        assertFalse(blockFormat.novaeng$isGeometryExposed());
        assertFalse(MixinGeometryDisplayLists.isStableModel(blockFormat));
    }

    @Test
    void originalExactClassWhitelistRemainsExact() {
        final SimpleBakedModel vanilla = new SimpleBakedModel(List.of(), Map.of(), true, false, null,
            ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE);
        assertTrue(MixinGeometryDisplayLists.isStableModel(vanilla));
        final SimpleBakedModel custom = new SimpleBakedModel(List.of(), Map.of(), true, false, null,
            ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE) {
        };
        assertFalse(MixinGeometryDisplayLists.isStableModel(custom));
    }

    @Test
    void actualTransformationUsesDirectCallsWithoutCallbackOrOperationAllocation() throws Exception {
        final byte[] bytes = IsolatedMixinTransformer.transformInPackage("github.kasuminova.novaeng.mixin",
            new String[]{TARGET}, "geometry.actinium.MixinGeometryDisplayLists")[0];
        final ClassNode transformed = new ClassNode();
        new ClassReader(bytes).accept(transformed, 0);
        int checked = 0;
        for (final MethodNode method : transformed.methods) {
            if (!method.name.equals("isStableModel") && !method.name.equals("getOrCompile")
                && !method.name.contains("novaeng$format") && !method.name.contains("novaeng$colors")) continue;
            checked++;
            for (final var instruction : method.instructions) {
                if (instruction instanceof TypeInsnNode type) {
                    assertFalse(type.desc.startsWith("org/spongepowered/asm/mixin/injection/callback/"), method.name);
                    assertFalse(type.getOpcode() == Opcodes.ANEWARRAY && type.desc.equals("java/lang/Object"), method.name);
                } else if (instruction instanceof MethodInsnNode call) {
                    assertFalse(call.owner.startsWith("com/llamalad7/mixinextras/injector/wrapoperation/"), method.name);
                    assertFalse(call.owner.equals("java/lang/Integer")
                        && (call.name.equals("valueOf") || call.name.equals("intValue")), method.name);
                }
            }
        }
        assertTrue(checked >= 4, "Check the predicate, caller, redirect handlers and any argument-capture bridges");
        assertEquals(1, transformed.methods.stream().filter(method -> method.name.equals("isStableModel")).count());
        assertEquals(1, transformed.methods.stream().filter(method -> method.name.equals("getOrCompile")).count());
        assertEquals(1, transformed.methods.stream().filter(method -> method.name.contains("novaeng$format"))
            .mapToLong(method -> originalCalls(method, "getCacheability")).sum());
        assertEquals(1, transformed.methods.stream().filter(method -> method.name.contains("novaeng$colors"))
            .mapToLong(method -> originalCalls(method, "getQuadColors")).sum(),
            "Dynamic tint sampling retains its original helper");
    }
}
