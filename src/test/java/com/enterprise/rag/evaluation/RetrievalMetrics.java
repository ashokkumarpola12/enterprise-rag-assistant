
package com.enterprise.rag.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RetrievalMetrics {

    public static double precisionAtK(
            List<Integer> retrievedChunkIndexes,
            List<Integer> expectedRelevantChunkIndexes,
            int k
    ) {
        if (k <= 0) {
            throw new IllegalArgumentException("K must be greater than zero");
        }

        Set<Integer> relevant =
                new HashSet<>(expectedRelevantChunkIndexes);

        long relevantRetrieved = retrievedChunkIndexes.stream()
                .limit(k)
                .filter(relevant::contains)
                .distinct()
                .count();

        return (double) relevantRetrieved / k;
    }


    public static double recallAtK(
            List<Integer> retrievedChunkIndexes,
            List<Integer> expectedRelevantChunkIndexes,
            int k
    ) {
        if (k <= 0) {
            throw new IllegalArgumentException("K must be greater than zero");
        }

        Set<Integer> relevant =
                new HashSet<>(expectedRelevantChunkIndexes);

        if (relevant.isEmpty()) {
            return 0.0;
        }

        long relevantRetrieved = retrievedChunkIndexes.stream()
                .limit(k)
                .filter(relevant::contains)
                .distinct()
                .count();

        return (double) relevantRetrieved / relevant.size();
    }

}
