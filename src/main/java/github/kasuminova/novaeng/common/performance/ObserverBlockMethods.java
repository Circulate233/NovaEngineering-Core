package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class ObserverBlockMethods {
    private static final ClassValue<Kind> TYPES = new ClassValue<>() {
        @Override
        protected Kind computeValue(final Class<?> type) {
            try {
                final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                return lookup.revealDirect(lookup.findVirtual(type, "observedNeighborChange",
                    MethodType.methodType(void.class, IBlockState.class, World.class, BlockPos.class,
                        Block.class, BlockPos.class))).getDeclaringClass() == Block.class ? Kind.EMPTY : Kind.CALLBACK;
            } catch (final ReflectiveOperationException unavailable) {
                return Kind.CALLBACK;
            }
        }
    };

    private ObserverBlockMethods() {
    }

    public static boolean isEmpty(final IBlockState state) {
        return state != null && state.getClass() == BlockStateContainer.StateImplementation.class
            && TYPES.get(state.getBlock().getClass()) == Kind.EMPTY;
    }

    private enum Kind {EMPTY, CALLBACK}
}
