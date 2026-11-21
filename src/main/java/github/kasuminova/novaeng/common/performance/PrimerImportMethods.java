package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

/**
 * Restricts buffered construction to getters whose default implementations only read fields.
 */
public record PrimerImportMethods(boolean supported) {
    private static final Method MATERIAL = ObfuscationReflectionHelper.findMethod(Block.class, "func_149688_o",
        Material.class, IBlockState.class);
    private static final Method TICK_RANDOMLY = ObfuscationReflectionHelper.findMethod(Block.class, "func_149653_t",
        boolean.class);
    private static final ClassValue<PrimerImportMethods> TYPES = new ClassValue<>() {
        @Override
        protected PrimerImportMethods computeValue(final Class<?> type) {
            try {
                return new PrimerImportMethods(isDefault(type, MATERIAL) && isDefault(type, TICK_RANDOMLY));
            } catch (ReflectiveOperationException | LinkageError | SecurityException unavailable) {
                return new PrimerImportMethods(false);
            }
        }
    };

    private static boolean isDefault(final Class<?> type, final Method method) throws NoSuchMethodException {
        return type.getMethod(method.getName(), method.getParameterTypes()).getDeclaringClass() == Block.class;
    }

    public static boolean supports(final Block block) {
        return TYPES.get(block.getClass()).supported;
    }
}
