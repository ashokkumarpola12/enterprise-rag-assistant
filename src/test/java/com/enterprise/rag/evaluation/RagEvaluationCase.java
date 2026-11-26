package com.enterprise.rag.evaluation;

import java.util.List;

public record RagEvaluationCase(
        String question,
        List<Integer> expectedRelevantChunkIndexes,
        boolean answerable
) {
}
