package github.kasuminova.novaeng.mixin.tconstruct;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MaterialWrapperAllocationTest {
    private static Object implementation(final Material material) throws Exception {
        final Map<String, String> names = new HashMap<>();
        names.put("net/minecraft/item/ItemStack", internal(Stack.class));
        names.put("net/minecraftforge/fluids/Fluid", internal(Fluid.class));
        names.put("net/minecraftforge/fluids/FluidStack", internal(FluidStack.class));
        names.put("slimeknights/tconstruct/library/materials/Material", internal(Material.class));
        names.put("slimeknights/tconstruct/library/smeltery/MeltingRecipe", internal(Recipe.class));
        names.put("slimeknights/mantle/util/RecipeMatch", internal(Match.class));
        names.put("slimeknights/tconstruct/library/tools/IToolPart", internal(Part.class));
        names.put("slimeknights/tconstruct/library/TinkerRegistry", internal(Registry.class));
        names.put("github/kasuminova/novaeng/common/performance/MeltingRecipeIndex", internal(Registry.class));
        final ClassNode node = new ClassNode();
        try (final var input = MaterialWrapperAllocationTest.class.getResourceAsStream("MixinMaterialMeltingInputs.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(new ClassRemapper(node, new SimpleRemapper(names)), 0);
        }
        node.access &= ~Opcodes.ACC_ABSTRACT;
        final ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        final byte[] bytes = writer.toByteArray();
        final Class<?> type = new ClassLoader(MaterialWrapperAllocationTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
        final Object instance = type.getConstructor().newInstance();
        final var field = type.getDeclaredField("material");
        field.setAccessible(true);
        field.set(instance, material);
        return instance;
    }

    private static String internal(final Class<?> type) {
        return type.getName().replace('.', '/');
    }

    @Test
    void materialInputsRetainFlatteningOrderDuplicatesAndLivePredicates() throws Exception {
        final Material material = new Material();
        material.fluid = new Fluid("a");
        material.representative = new Stack("representative");
        Registry.recipes.clear();
        Registry.recipes.add(new Recipe(new Fluid("a"), new Stack("one"), new Stack("one")));
        Registry.recipes.add(new Recipe(new Fluid("other"), new Stack("excluded")));
        final Object probe = implementation(material);
        final var getInputs = probe.getClass().getDeclaredMethod("getInputs");
        getInputs.setAccessible(true);
        final List<?> first = (List<?>) getInputs.invoke(probe);
        assertEquals(List.of(new Stack("one"), new Stack("one"), material.representative), first);
        first.clear();
        assertEquals(3, ((List<?>) getInputs.invoke(probe)).size());
        material.fluid = new Fluid("other");
        assertEquals(List.of(new Stack("excluded"), material.representative), getInputs.invoke(probe));
        material.fluid = null;
        material.representative = new Stack("");
        assertEquals(List.of(), getInputs.invoke(probe));
        material.representative = null;
        assertEquals(List.of(), getInputs.invoke(probe));
    }

    @Test
    void outputsRunDynamicCallbacksInRegistryOrderOnEachCall() throws Exception {
        final Material material = new Material();
        final List<String> calls = new ArrayList<>();
        Registry.parts.clear();
        for (final String name : List.of("one", "two")) {
            Registry.parts.add(new Part() {
                public boolean canUseMaterial(final Material value) {
                    calls.add("check:" + name);
                    return value.representative != null;
                }

                public Stack getItemstackWithMaterial(final Material value) {
                    calls.add("make:" + name);
                    return new Stack(name);
                }
            });
        }
        final Object probe = implementation(material);
        final var getOutputs = probe.getClass().getDeclaredMethod("getOutputs");
        getOutputs.setAccessible(true);
        material.representative = new Stack("enabled");
        assertEquals(List.of(new Stack("one"), new Stack("two")), getOutputs.invoke(probe));
        assertEquals(List.of("check:one", "make:one", "check:two", "make:two"), calls);
        calls.clear();
        material.representative = null;
        assertEquals(List.of(), getOutputs.invoke(probe));
        assertEquals(List.of("check:one", "check:two"), calls);
    }

    public interface Part {
        boolean canUseMaterial(Material material);

        Stack getItemstackWithMaterial(Material material);
    }

    public record Stack(String value) {
        public boolean isEmpty() {
            return value.isEmpty();
        }
    }

    public record Fluid(String name) {
    }

    public record FluidStack(Fluid value) {
        public Fluid getFluid() {
            return value;
        }
    }

    public record Match(List<Stack> stacks) {
        public List<Stack> getInputs() {
            return stacks;
        }
    }

    public static final class Recipe {
        public FluidStack output;
        public Match input;

        Recipe(final Fluid fluid, final Stack... stacks) {
            output = new FluidStack(fluid);
            input = new Match(List.of(stacks));
        }
    }

    public static final class Material {
        public Fluid fluid;
        public Stack representative;

        public boolean hasFluid() {
            return fluid != null;
        }

        public Fluid getFluid() {
            return fluid;
        }

        public Stack getRepresentativeItem() {
            return representative;
        }
    }

    public static final class Registry {
        public static final List<Recipe> recipes = new ArrayList<>();
        public static final Set<Part> parts = new LinkedHashSet<>();

        public static List<Recipe> candidates(final Fluid fluid) {
            return recipes;
        }

        public static Set<Part> getToolParts() {
            return parts;
        }
    }
}
