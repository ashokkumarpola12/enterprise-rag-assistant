package com.enterprise.rag.evaluation;

import com.enterprise.rag.dto.RagAnswerResponse;
import com.enterprise.rag.dto.SemanticSearchResponse;
import com.enterprise.rag.service.RagService;
import com.enterprise.rag.service.SemanticSearchService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RagRetrievalEvaluationTest {

    @Autowired
    private SemanticSearchService semanticSearchService;

    @Autowired
    private RagService ragService;

    @Test
    void shouldLoadSemanticSearchService() {

        assertNotNull(semanticSearchService);

        System.out.println(
                "SemanticSearchService initialized successfully"
        );
    }

    @Test
    void shouldRetrieveChunksForEvaluationQuestion() {

        String question =
                "How does Spring Boot handle authentication and authorization?";

        int topK = 3;

        List<SemanticSearchResponse> retrievedChunks =
                semanticSearchService.search(question, topK);

        assertNotNull(retrievedChunks);
        assertFalse(retrievedChunks.isEmpty());

        System.out.println("Question: " + question);
        System.out.println("Retrieved chunks:");

        retrievedChunks.forEach(chunk ->
                System.out.printf(
                        "Document ID: %s | Chunk Index: %d | Distance: %.4f%n",
                        chunk.documentId(),
                        chunk.chunkIndex(),
                        chunk.distance()
                )
        );
    }


    @Test
    void shouldCalculateActualRetrievalMetrics() {

        String question =
                "How does Spring Boot handle authentication and authorization?";

        List<Integer> expectedRelevant = List.of(4);

        int topK = 3;

        List<SemanticSearchResponse> retrievedChunks =
                semanticSearchService.search(question, topK);

        List<Integer> retrievedIndexes = retrievedChunks.stream()
                .map(SemanticSearchResponse::chunkIndex)
                .toList();

        double precision = RetrievalMetrics.precisionAtK(
                retrievedIndexes,
                expectedRelevant,
                topK
        );

        double recall = RetrievalMetrics.recallAtK(
                retrievedIndexes,
                expectedRelevant,
                topK
        );

        System.out.println("Retrieved indexes: " + retrievedIndexes);
        System.out.printf("Precision@%d: %.2f%%%n", topK, precision * 100);
        System.out.printf("Recall@%d: %.2f%%%n", topK, recall * 100);

        assertTrue(precision >= 0.0 && precision <= 1.0);
        assertTrue(recall >= 0.0 && recall <= 1.0);
    }


    @Test
    void shouldEvaluateQuestionsFromDataset() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(
                        "rag-evaluation/evaluation-dataset.json"
                )) {

            assertNotNull(inputStream, "Evaluation dataset not found");

            List<RagEvaluationCase> testCases =
                    objectMapper.readValue(
                            inputStream,
                            new TypeReference<>() {
                            }
                    );

            int topK = 3;

            for (RagEvaluationCase testCase : testCases) {

                if (!testCase.answerable()) {
                    System.out.println(
                            "Skipping unanswerable question: " + testCase.question()
                    );
                    continue;
                }

                List<SemanticSearchResponse> retrievedChunks =
                        semanticSearchService.search(
                                testCase.question(),
                                topK
                        );

                List<Integer> retrievedIndexes = retrievedChunks.stream()
                        .map(SemanticSearchResponse::chunkIndex)
                        .toList();

                double precision = RetrievalMetrics.precisionAtK(
                        retrievedIndexes,
                        testCase.expectedRelevantChunkIndexes(),
                        topK
                );

                double recall = RetrievalMetrics.recallAtK(
                        retrievedIndexes,
                        testCase.expectedRelevantChunkIndexes(),
                        topK
                );

                assertTrue(
                        recall >= 1.0,
                        "Expected all relevant chunks in top " + topK
                                + " for question: " + testCase.question()
                );

                assertTrue(
                        precision > 0.0,
                        "Expected at least one relevant chunk for question: "
                                + testCase.question()
                );


                System.out.println("--------------------------------");
                System.out.println("Question: " + testCase.question());
                System.out.println("Retrieved indexes: " + retrievedIndexes);
                System.out.printf("Precision@%d: %.2f%%%n", topK, precision * 100);
                System.out.printf("Recall@%d: %.2f%%%n", topK, recall * 100);
            }
        }
    }


    @Test
    void shouldRejectUnanswerableQuestion() {

        String question =
                "What is the annual revenue of Microsoft in 2025?";

        RagAnswerResponse response = ragService.answerQuestion(question);

        assertNotNull(response);
        assertTrue(
                response.sources().isEmpty(),
                "Unanswerable questions should not return sources"
        );

        assertEquals(
                "I couldn't find relevant information in the knowledge base.",
                response.answer()
        );

        System.out.println("Question: " + question);
        System.out.println("Answer: " + response.answer());
        System.out.println("Sources: " + response.sources().size());
    }


    @Test
    void shouldEvaluateUnanswerableQuestionsFromDataset() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(
                        "rag-evaluation/evaluation-dataset.json"
                )) {

            assertNotNull(inputStream, "Evaluation dataset not found");

            List<RagEvaluationCase> testCases =
                    objectMapper.readValue(
                            inputStream,
                            new TypeReference<>() {
                            }
                    );

            int evaluated = 0;
            int correctlyRejected = 0;

            for (RagEvaluationCase testCase : testCases) {

                if (testCase.answerable()) {
                    continue;
                }

                evaluated++;


                RagAnswerResponse response =
                        ragService.answerQuestion(testCase.question());

                System.out.println("Actual answer: " + response.answer());
                System.out.println("Actual sources: " + response.sources());
                System.out.println("Sources empty: " + response.sources().isEmpty());

                boolean rejected = response.sources().isEmpty();

                System.out.println("Correctly rejected: " + rejected);

                if (rejected) {
                    correctlyRejected++;
                }

                assertTrue(
                        rejected,
                        "Failed to reject unsupported question: "
                                + testCase.question()
                );

            }

            assertTrue(evaluated > 0, "No unanswerable evaluation cases found");

            double rejectionRate = (double) correctlyRejected / evaluated;

            System.out.printf(
                    "Unanswerable rejection rate: %.2f%%%n",
                    rejectionRate * 100
            );
        }
    }



}
