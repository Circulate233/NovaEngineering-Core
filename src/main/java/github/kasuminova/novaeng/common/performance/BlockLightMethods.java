package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Class-level dispatch metadata; light values themselves remain live.
 */
public record BlockLightMethods(boolean defaultOpacity, boolean defaultValue) {
    private static final MethodType CONTEXT_METHOD = MethodType.methodType(int.class,
        IBlockState.class, IBlockAccess.class, BlockPos.class);
    private static final ClassValue<BlockLightMethods> TYPES = new ClassValue<>() {
        @Override
        protected BlockLightMethods computeValue(final Class<?> type) {
            try {
                final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                return new BlockLightMethods(
                    lookup.revealDirect(lookup.findVirtual(type, "getLightOpacity", CONTEXT_METHOD))
                          .getDeclaringClass() == Block.class,
                    lookup.revealDirect(lookup.findVirtual(type, "getLightValue", CONTEXT_METHOD))
                          .getDeclaringClass() == Block.class);
            } catch (final ReflectiveOperationException unavailable) {
                return new BlockLightMethods(false, false);
            }
        }
    };

    public static BlockLightMethods of(final Block block) {
        return TYPES.get(block.getClass());
    }
}
