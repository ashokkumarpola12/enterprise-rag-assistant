
package com.enterprise.rag.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RetrievalMetricsTest {

    @Test
    void shouldCalculatePrecisionAt3() {

        List<Integer> retrieved = List.of(4, 0, 1);
        List<Integer> expectedRelevant = List.of(4);

        double precision = RetrievalMetrics.precisionAtK(
                retrieved,
                expectedRelevant,
                3
        );

        assertEquals(1.0 / 3.0, precision, 0.0001);
    }

    @Test
    void shouldReturnOneWhenAllTopKChunksAreRelevant() {

        List<Integer> retrieved = List.of(4, 2, 1);
        List<Integer> expectedRelevant = List.of(4, 2, 1);

        double precision = RetrievalMetrics.precisionAtK(
                retrieved,
                expectedRelevant,
                3
        );

        assertEquals(1.0, precision, 0.0001);
    }

    @Test
    void shouldReturnZeroWhenNoChunksAreRelevant() {

        List<Integer> retrieved = List.of(0, 1, 2);
        List<Integer> expectedRelevant = List.of(4);

        double precision = RetrievalMetrics.precisionAtK(
                retrieved,
                expectedRelevant,
                3
        );

        assertEquals(0.0, precision, 0.0001);
    }

    @Test
    void shouldHandleFewerResultsThanK() {

        List<Integer> retrieved = List.of(4, 0);
        List<Integer> expectedRelevant = List.of(4);

        double precision = RetrievalMetrics.precisionAtK(
                retrieved,
                expectedRelevant,
                3
        );

        // Fixed-K precision: 1 relevant result / 3 requested
        assertEquals(1.0 / 3.0, precision, 0.0001);
    }

    @Test
    void shouldRejectInvalidK() {

        assertThrows(
                IllegalArgumentException.class,
                () -> RetrievalMetrics.precisionAtK(
                        List.of(4),
                        List.of(4),
                        0
                )
        );
    }


    @Test
    void shouldCalculateRecallAt3() {

        List<Integer> retrieved = List.of(4, 0, 1);
        List<Integer> expectedRelevant = List.of(4, 2);

        double recall = RetrievalMetrics.recallAtK(
                retrieved,
                expectedRelevant,
                3
        );

        assertEquals(0.5, recall, 0.0001);
    }

    @Test
    void shouldReturnOneWhenAllRelevantChunksAreRetrieved() {

        List<Integer> retrieved = List.of(4, 2, 0);
        List<Integer> expectedRelevant = List.of(4, 2);

        double recall = RetrievalMetrics.recallAtK(
                retrieved,
                expectedRelevant,
                3
        );

        assertEquals(1.0, recall, 0.0001);
    }

    @Test
    void shouldReturnZeroWhenNoRelevantChunksAreRetrieved() {

        List<Integer> retrieved = List.of(0, 1, 3);
        List<Integer> expectedRelevant = List.of(4, 2);

        double recall = RetrievalMetrics.recallAtK(
                retrieved,
                expectedRelevant,
                3
        );

        assertEquals(0.0, recall, 0.0001);
    }

    @Test
    void shouldHandleEmptyRelevantChunks() {

        List<Integer> retrieved = List.of(0, 1, 3);
        List<Integer> expectedRelevant = List.of();

        double recall = RetrievalMetrics.recallAtK(
                retrieved,
                expectedRelevant,
                3
        );

        assertEquals(0.0, recall, 0.0001);
    }

}
