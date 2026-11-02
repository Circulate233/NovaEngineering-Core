package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class LeafBlockKinds {
    public static final int DYNAMIC = -3;
    private static final MethodType SIGNATURE = MethodType.methodType(boolean.class, IBlockState.class, IBlockAccess.class, BlockPos.class);
    private static final ClassValue<Integer> KINDS = new ClassValue<>() {
        @Override
        protected Integer computeValue(final Class<?> type) {
            try {
                final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                final Class<?> support = lookup.revealDirect(lookup.findVirtual(type, "canSustainLeaves", SIGNATURE)).getDeclaringClass();
                if (support == BlockLog.class) return 0;
                if (support != Block.class) return DYNAMIC;
                final Class<?> leaves = lookup.revealDirect(lookup.findVirtual(type, "isLeaves", SIGNATURE)).getDeclaringClass();
                return leaves == BlockLeaves.class ? -2 : leaves == Block.class ? -1 : DYNAMIC;
            } catch (final ReflectiveOperationException unavailable) {
                return DYNAMIC;
            }
        }
    };

    private LeafBlockKinds() {
    }

    public static int of(final Block block) {
        return block instanceof LeafBlockKindAccess classified ? classified.nova$leafKind() : classify(block.getClass());
    }

    public static int classify(final Class<?> type) {
        return KINDS.get(type);
    }
}
