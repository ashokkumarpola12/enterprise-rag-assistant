package com.enterprise.rag.controller;

import com.enterprise.rag.ingestion.MarkdownIngestionCoordinator;
import com.enterprise.rag.service.DocumentIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents/markdown")
public class MarkdownIngestionController {

    private final MarkdownIngestionCoordinator coordinator;
    private final DocumentIngestionService documentIngestionService;

    public MarkdownIngestionController(
            MarkdownIngestionCoordinator coordinator,
            DocumentIngestionService documentIngestionService) {
        this.coordinator = coordinator;
        this.documentIngestionService = documentIngestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, UUID>> ingestMarkdownDocuments()
            throws IOException {

        Map<String, UUID> results = coordinator.ingestAll();

        return ResponseEntity.ok(results);
    }

    @PostMapping("/{documentId}/embeddings")
    public ResponseEntity<Map<String,Object>> ingestMarkdownEmbeddings(
            @PathVariable UUID documentId
    ){
        int processedCount = documentIngestionService.generateEmbeddingsForDocument(documentId);
        return ResponseEntity.ok(Map.of("documentId",documentId,
                "embeddingsGenerated",processedCount));
    }
}