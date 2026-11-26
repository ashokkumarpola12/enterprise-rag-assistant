package com.enterprise.rag.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveChunkWithJsonMetadata() {

        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        documentRepository.saveDocument(
                documentId,
                "test-runbook.md",
                "text/markdown"
        );

        String metadataJson = """
                {
                  "filename": "test-runbook.md",
                  "documentTitle": "Test Runbook",
                  "sectionHeading": "Diagnosis",
                  "chunkIndex": 0
                }
                """;

        documentRepository.saveChunk(
                chunkId,
                documentId,
                0,
                "## Diagnosis\nCheck database connections.",
                metadataJson
        );

        String sectionHeading = jdbcTemplate.queryForObject(
                """
                SELECT metadata->>'sectionHeading'
                FROM document_chunks
                WHERE id = ?
                """,
                String.class,
                chunkId
        );

        assertEquals("Diagnosis", sectionHeading);
    }

    @Test
    void shouldDetectExistingDocument() {

        UUID documentId = UUID.randomUUID();

        documentRepository.saveDocument(
                documentId,
                "duplicate-check-test.md",
                "text/markdown"
        );

        assertTrue(
                documentRepository.existsByFilenameAndContentType(
                        "duplicate-check-test.md",
                        "text/markdown"
                )
        );

        assertFalse(
                documentRepository.existsByFilenameAndContentType(
                        "nonexistent-runbook.md",
                        "text/markdown"
                )
        );
    }
}