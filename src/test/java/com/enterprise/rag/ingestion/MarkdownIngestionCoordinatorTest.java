package com.enterprise.rag.ingestion;

import com.enterprise.rag.service.DocumentIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTest
class MarkdownIngestionCoordinatorTest {

    @Autowired
    private MarkdownIngestionCoordinator coordinator;

    @MockitoBean
    private DocumentIngestionService ingestionService;

    @Test
    void shouldCoordinateAllFourRunbooks() throws Exception {

        // Return a UUID whenever a runbook is ingested.
        when(ingestionService.ingestMarkdownChunks(anyList()))
                .thenAnswer(invocation -> UUID.randomUUID());

        // Execute coordinator logic.
        Map<String, UUID> results = coordinator.ingestAll();

        // Verify four documents were processed.
        assertEquals(4, results.size());

        // Verify persistence service was called four times.
        verify(ingestionService, times(4))
                .ingestMarkdownChunks(anyList());

        // Verify each expected filename.
        assertTrue(results.containsKey("01-payments-high-latency.md"));
        assertTrue(results.containsKey("02-auth-login-failures.md"));
        assertTrue(results.containsKey("03-postgres-replication-lag.md"));
        assertTrue(results.containsKey("04-aks-crashloopbackoff.md"));

        results.forEach((filename, documentId) ->
                System.out.println(filename + " -> " + documentId)
        );
    }
}