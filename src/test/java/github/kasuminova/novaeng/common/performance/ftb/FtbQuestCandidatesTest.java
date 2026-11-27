package github.kasuminova.novaeng.common.performance.ftb;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FtbQuestCandidatesTest {
    @Test
    void retainedCallbackListsStayIntactAcrossInvocations() {
        List<List<Object>> retained = new ObjectArrayList<>();
        Object stack = new Object();
        List<Object> firstOutput = new ObjectArrayList<>();
        for (int i = 0; i < 2; i++) {
            FtbQuestCandidates.appendFresh(ObjectArrayList::new, candidates -> {
                assertTrue(candidates.isEmpty());
                retained.add(candidates);
                candidates.add(stack);
                candidates.add(stack);
            }, candidate -> true, i == 0 ? firstOutput : new ObjectArrayList<>());
        }
        assertNotSame(retained.get(0), retained.get(1));
        assertEquals(List.of(stack, stack), retained.get(0));
        retained.get(0).clear();
        assertEquals(List.of(stack, stack), firstOutput);
        assertEquals(List.of(stack, stack), retained.get(1));
        assertSame(stack, firstOutput.get(0));
    }

    @Test
    void completesEnumerationBeforeFilteringAndPreservesDuplicatesAndOrder() {
        List<String> calls = new ObjectArrayList<>();
        List<String> output = new ObjectArrayList<>();
        FtbQuestCandidates.appendFresh(ObjectArrayList::new, candidates -> {
            calls.add("enumerate");
            candidates.addAll(List.of("a", "b", "a"));
            calls.add("enumerated");
        }, candidate -> {
            calls.add(candidate);
            return candidate.equals("a");
        }, output);
        assertEquals(List.of("enumerate", "enumerated", "a", "b", "a"), calls);
        assertEquals(List.of("a", "a"), output);
    }

    @Test
    void callbackFailuresDoNotPublishPartialResultsOrClearEscapedCandidates() {
        List<List<String>> retained = new ObjectArrayList<>();
        List<String> output = new ObjectArrayList<>(List.of("existing"));
        IllegalStateException failure = new IllegalStateException();
        assertSame(failure, assertThrows(IllegalStateException.class, () ->
            FtbQuestCandidates.appendFresh(ObjectArrayList::new, candidates -> {
                retained.add(candidates);
                candidates.addAll(List.of("a", "b"));
            }, candidate -> {
                if (candidate.equals("b")) {
                    throw failure;
                }
                return true;
            }, output)));
        assertEquals(List.of("existing"), output);
        assertEquals(List.of("a", "b"), retained.get(0));
        assertSame(failure, assertThrows(IllegalStateException.class, () ->
            FtbQuestCandidates.appendFresh(ObjectArrayList::new, candidates -> {
                retained.add(candidates);
                candidates.add("retained");
                throw failure;
            }, candidate -> fail("Filtering started after enumeration failed"), output)));
        assertEquals(List.of("existing"), output);
        assertEquals(List.of("retained"), retained.get(1));
    }

    @Test
    void nestedExpansionsHaveIndependentCandidates() {
        List<String> output = new ObjectArrayList<>();
        FtbQuestCandidates.appendFresh(ObjectArrayList::new, outer -> {
            outer.add("outer");
            FtbQuestCandidates.appendFresh(ObjectArrayList::new, inner -> {
                assertNotSame(outer, inner);
                inner.add("inner");
            }, candidate -> true, output);
            assertEquals(List.of("outer"), outer);
        }, candidate -> true, output);
        assertEquals(List.of("inner", "outer"), output);
    }
}
