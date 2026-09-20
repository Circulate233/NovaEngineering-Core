package github.kasuminova.novaeng.common.gamerule;

import java.util.Optional;

/** Allocation-free cached-result implementation of {@link GameRuleBooleanFastPath}. */
public final class GameRuleBooleanFastPathImpl implements GameRuleBooleanFastPath {

    private static final Optional<Result> TRUE = Optional.of(new Result(true, 1));
    private static final Optional<Result> FALSE = Optional.of(new Result(false, 0));
    private static final GameRuleBooleanFastPath INSTANCE = new GameRuleBooleanFastPathImpl();

    private GameRuleBooleanFastPathImpl() {
    }

    /** Returns the shared stateless matcher. */
    public static GameRuleBooleanFastPath instance() {
        return INSTANCE;
    }

    @Override
    public Optional<Result> match(final String value) {
        if ("true".equalsIgnoreCase(value)) {
            return TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return FALSE;
        }
        return Optional.empty();
    }
}
