package com.enterprise.rag.ingestion;

import com.enterprise.rag.dto.MarkdownChunk;
import com.enterprise.rag.service.DocumentIngestionService;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MarkdownIngestionCoordinator {

    private final MarkdownIngestionPipeline pipeline;
    private final DocumentIngestionService ingestionService;

    public MarkdownIngestionCoordinator(
            MarkdownIngestionPipeline pipeline,
            DocumentIngestionService ingestionService
    ) {
        this.pipeline = pipeline;
        this.ingestionService = ingestionService;
    }

    public Map<String, UUID> ingestAll()
            throws IOException, JacksonException {

        // 1. Prepare all Markdown chunks
        List<MarkdownChunk> allChunks = pipeline.prepareChunks();

        // 2. Group chunks by filename
        Map<String, List<MarkdownChunk>> groupedChunks =
                allChunks.stream()
                        .collect(Collectors.groupingBy(
                                MarkdownChunk::filename,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        Map<String, UUID> results = new LinkedHashMap<>();

        // 3. Ingest each runbook independently
        for (Map.Entry<String, List<MarkdownChunk>> entry
                : groupedChunks.entrySet()) {

            UUID documentId =
                    ingestionService.ingestMarkdownChunks(entry.getValue());

            results.put(entry.getKey(), documentId);
        }

        return results;
    }
}