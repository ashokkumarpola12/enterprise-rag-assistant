
package com.enterprise.rag.evaluation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RagEvaluationDatasetTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadEvaluationDataset() throws Exception {

        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(
                        "rag-evaluation/evaluation-dataset.json"
                )) {

            assertNotNull(inputStream, "Evaluation dataset file not found");

            List<RagEvaluationCase> testCases =
                    objectMapper.readValue(
                            inputStream,
                            new TypeReference<>() {
                            }
                    );

            assertEquals(6, testCases.size());

            assertEquals(
                    "How does Spring Boot handle authentication and authorization?",
                    testCases.getFirst().question()
            );

            assertEquals(
                    List.of(4),
                    testCases.get(0).expectedRelevantChunkIndexes()
            );

            assertTrue(testCases.get(0).answerable());
            assertFalse(testCases.get(1).answerable());

            System.out.println("Evaluation cases loaded: " + testCases.size());
        }
    }
}
