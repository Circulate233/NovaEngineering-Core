package github.kasuminova.novaeng.common.gamerule;

import java.util.Optional;

/**
 * Recognizes only boolean game-rule strings whose original numeric parses are guaranteed to fail.
 */
public interface GameRuleBooleanFastPath {

    /**
     * Returns the original boolean and integer field values for a safe fast-path input.
     *
     * @param value incoming game-rule string, including possible {@code null}
     * @return a result only for case-insensitive {@code true} or {@code false}
     */
    Optional<Result> match(String value);

    /** Exact field values produced before vanilla's failed integer and double parses. */
    record Result(boolean booleanValue, int integerValue) {
    }
}
