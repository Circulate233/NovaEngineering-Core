package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Restricts buffered construction to getters whose default implementations only read fields.
 */
public record PrimerImportMethods(boolean supported) {
    private static final ClassValue<PrimerImportMethods> TYPES = new ClassValue<>() {
        @Override
        protected PrimerImportMethods computeValue(final Class<?> type) {
            try {
                final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                return new PrimerImportMethods(isDefault(lookup, type, "getMaterial", "func_149688_o",
                    MethodType.methodType(Material.class, IBlockState.class))
                    && isDefault(lookup, type, "getTickRandomly", "func_149653_t",
                    MethodType.methodType(boolean.class)));
            } catch (final ReflectiveOperationException unavailable) {
                return new PrimerImportMethods(false);
            }
        }
    };

    private static boolean isDefault(final MethodHandles.Lookup lookup, final Class<?> type,
                                     final String named, final String srg, final MethodType signature)
        throws ReflectiveOperationException {
        try {
            return lookup.revealDirect(lookup.findVirtual(type, named, signature)).getDeclaringClass() == Block.class;
        } catch (final NoSuchMethodException obfuscated) {
            return lookup.revealDirect(lookup.findVirtual(type, srg, signature)).getDeclaringClass() == Block.class;
        }
    }

    public static boolean supports(final Block block) {
        return TYPES.get(block.getClass()).supported;
    }
}
