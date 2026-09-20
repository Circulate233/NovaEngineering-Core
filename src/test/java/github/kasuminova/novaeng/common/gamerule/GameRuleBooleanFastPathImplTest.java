package github.kasuminova.novaeng.common.gamerule;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRuleBooleanFastPathImplTest {

    private final GameRuleBooleanFastPath fastPath = GameRuleBooleanFastPathImpl.instance();

    @Test
    void recognizesAllBooleanCaseVariants() {
        final Map<String, GameRuleBooleanFastPath.Result> values = Map.of(
            "true", new GameRuleBooleanFastPath.Result(true, 1),
            "TRUE", new GameRuleBooleanFastPath.Result(true, 1),
            "TrUe", new GameRuleBooleanFastPath.Result(true, 1),
            "false", new GameRuleBooleanFastPath.Result(false, 0),
            "FALSE", new GameRuleBooleanFastPath.Result(false, 0),
            "FaLsE", new GameRuleBooleanFastPath.Result(false, 0));

        values.forEach((input, expected) -> assertEquals(expected, this.fastPath.match(input).orElseThrow()));
    }

    @Test
    void rejectsEverythingThatMustUseVanillaParsing() {
        for (final String value : new String[]{"1", "0", " true", "false ", "", "NaN", "Infinity", "+1"}) {
            assertTrue(this.fastPath.match(value).isEmpty(), value);
        }
        assertTrue(this.fastPath.match(null).isEmpty());
    }
}
