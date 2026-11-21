package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

/** Reuses one block's extension only within the public getSprites call that owns it. */
public final class JourneyMapStateMemo implements AutoCloseable {
    private static final ThreadLocal<JourneyMapStateMemo> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<JourneyMapStateMemo> IDLE = new ThreadLocal<>();
    private JourneyMapStateMemo previous;
    private Object world;
    private IBlockState state;
    private IBlockState result;
    private int x, y, z;
    private boolean populated;

    private JourneyMapStateMemo() {
    }

    public static JourneyMapStateMemo begin() {
        JourneyMapStateMemo memo = IDLE.get();
        if (memo == null) memo = new JourneyMapStateMemo();
        else IDLE.set(null);
        memo.previous = ACTIVE.get();
        ACTIVE.set(memo);
        return memo;
    }

    public static JourneyMapStateMemo current() {
        return ACTIVE.get();
    }

    public boolean matches(final Object world, final BlockPos pos, final IBlockState state) {
        return populated && this.world == world && this.state == state
            && x == pos.getX() && y == pos.getY() && z == pos.getZ();
    }

    public IBlockState result() {
        return result;
    }

    public void remember(final Object world, final BlockPos pos, final IBlockState state, final IBlockState result) {
        this.world = world;
        this.state = state;
        this.result = result;
        // JourneyMap is allowed to advance and reuse the caller's MutableBlockPos.
        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();
        populated = true;
    }

    @Override
    public void close() {
        populated = false;
        world = null;
        state = null;
        result = null;
        // Keep the ThreadLocal entry while clearing its value. remove/set would
        // allocate a new ThreadLocalMap.Entry for every mapped block.
        ACTIVE.set(previous);
        previous = null;
        if (IDLE.get() == null) IDLE.set(this);
    }
}
