package github.kasuminova.novaeng.common.gamerule;

/** Allocation-free cached-result implementation of {@link GameRuleBooleanFastPath}. */
public final class GameRuleBooleanFastPathImpl implements GameRuleBooleanFastPath {

    private static final Result TRUE = new Result(true, 1);
    private static final Result FALSE = new Result(false, 0);
    private static final GameRuleBooleanFastPath INSTANCE = new GameRuleBooleanFastPathImpl();

    private GameRuleBooleanFastPathImpl() {
    }

    /** Returns the shared stateless matcher. */
    public static GameRuleBooleanFastPath instance() {
        return INSTANCE;
    }

    @Override
    public Result match(final String value) {
        if ("true".equalsIgnoreCase(value)) {
            return TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return FALSE;
        }
        return null;
    }
}
