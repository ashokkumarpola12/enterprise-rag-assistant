package com.enterprise.rag.ingestion;

import com.enterprise.rag.dto.MarkdownChunk;
import com.enterprise.rag.service.DocumentIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DocumentIngestionServiceTest {

    @Autowired
    private DocumentIngestionService ingestionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistMarkdownChunksWithMetadata() throws Exception {

        List<MarkdownChunk> chunks = List.of(
                new MarkdownChunk(
                        "integration-test-runbook.md",
                        "Integration Test Runbook",
                        "Diagnosis",
                        0,
                        "## Diagnosis\nCheck application logs."
                ),
                new MarkdownChunk(
                        "integration-test-runbook.md",
                        "Integration Test Runbook",
                        "Resolution",
                        1,
                        "## Resolution\nRestart the affected service."
                )
        );

        UUID documentId = ingestionService.ingestMarkdownChunks(chunks);

        Integer documentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM documents WHERE id = ?",
                Integer.class,
                documentId
        );

        Integer chunkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM document_chunks WHERE document_id = ?",
                Integer.class,
                documentId
        );

        assertEquals(1, documentCount);
        assertEquals(2, chunkCount);

        String heading = jdbcTemplate.queryForObject(
                """
                SELECT metadata->>'sectionHeading'
                FROM document_chunks
                WHERE document_id = ? AND chunk_index = 0
                """,
                String.class,
                documentId
        );

        assertEquals("Diagnosis", heading);

        System.out.println("Document saved: " + documentId);
        System.out.println("Chunks saved: " + chunkCount);
        System.out.println("Metadata verified: " + heading);
    }

    @Test
    void shouldRejectDuplicateMarkdownDocument() throws Exception {

        List<MarkdownChunk> chunks = List.of(
                new MarkdownChunk(
                        "duplicate-ingestion-test.md",
                        "Duplicate Ingestion Test",
                        "Diagnosis",
                        0,
                        "## Diagnosis\nCheck application logs."
                )
        );

        // First ingestion should succeed.
        UUID documentId = ingestionService.ingestMarkdownChunks(chunks);

        assertNotNull(documentId);

        // Second ingestion of the same filename should fail.
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ingestionService.ingestMarkdownChunks(chunks)
        );

        assertTrue(
                exception.getMessage().contains("already exists")
        );

        // Verify that only one document record exists.
        Integer documentCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM documents
                WHERE filename = ?
                  AND content_type = ?
                """,
                Integer.class,
                "duplicate-ingestion-test.md",
                "text/markdown"
        );

        assertEquals(1, documentCount);

        System.out.println("Duplicate ingestion rejected successfully");
        System.out.println("Document count: " + documentCount);
    }

    @Autowired
    private MarkdownIngestionPipeline markdownIngestionPipeline;

    @Test
    void shouldIngestAllMarkdownRunbooks() throws Exception {

        // 1. Prepare all Markdown chunks
        List<MarkdownChunk> allChunks =
                markdownIngestionPipeline.prepareChunks();

        assertEquals(29, allChunks.size());

        // 2. Group chunks by filename
        Map<String, List<MarkdownChunk>> chunksByFilename =
                allChunks.stream()
                        .collect(Collectors.groupingBy(
                                MarkdownChunk::filename,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        assertEquals(4, chunksByFilename.size());

        // 3. Persist each runbook separately
        for (Map.Entry<String, List<MarkdownChunk>> entry
                : chunksByFilename.entrySet()) {

            UUID documentId =
                    ingestionService.ingestMarkdownChunks(entry.getValue());

            // 4. Verify chunks for this document
            Integer chunkCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM document_chunks
                    WHERE document_id = ?
                    """,
                    Integer.class,
                    documentId
            );

            assertEquals(entry.getValue().size(), chunkCount);

            System.out.println(
                    entry.getKey() + " → " + chunkCount + " chunks saved"
            );
        }

        System.out.println("Total runbooks processed: " + chunksByFilename.size());
        System.out.println("Total chunks processed: " + allChunks.size());
    }
}