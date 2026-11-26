package com.enterprise.rag.ingestion;

import com.enterprise.rag.dto.MarkdownChunk;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MarkdownSectionChunkerTest {

    @Autowired
    private MarkdownDocumentReader markdownDocumentReader;

    @Autowired
    private MarkdownSectionChunker markdownSectionChunker;

    @Autowired
    private MarkdownSizeAwareChunker markdownSizeAwareChunker;

    @Test
    void shouldSplitAllRunbooksIntoSections() throws IOException {

        List<Path> documents =
                markdownDocumentReader.discoverDocuments();

        assertEquals(4, documents.size());

        for (Path document : documents) {

            String content =
                    markdownDocumentReader.readDocument(document);

            List<String> chunks =
                    markdownSectionChunker.split(content);

            // Metadata + six standard runbook sections
            assertEquals(7, chunks.size(),
                    "Unexpected section count for: "
                            + document.getFileName());

            assertTrue(chunks.get(0).startsWith("# Runbook:"));
            assertTrue(chunks.get(1).startsWith("## Symptoms"));
            assertTrue(chunks.get(2).startsWith("## Likely causes"));
            assertTrue(chunks.get(3).startsWith("## Diagnosis"));
            assertTrue(chunks.get(4).startsWith("## Resolution"));
            assertTrue(chunks.get(5).startsWith("## Verification"));
            assertTrue(chunks.get(6).startsWith("## Prevention"));

            System.out.println("--------------------------------");
            System.out.println("Document: " + document.getFileName());
            System.out.println("Total chunks: " + chunks.size());

            for (int i = 0; i < chunks.size(); i++) {
                System.out.println(
                        "Chunk " + i + ": "
                                + chunks.get(i).lines().findFirst().orElse("")
                );
            }
        }
    }

    @Test
    void shouldPreserveDocumentContent() throws IOException {

        Path document =
                markdownDocumentReader.discoverDocuments().getFirst();

        String content =
                markdownDocumentReader.readDocument(document);

        List<String> chunks =
                markdownSectionChunker.split(content);

        String reconstructed =
                String.join("\n", chunks);

        assertEquals(
                content.replaceAll("\\s+", ""),
                reconstructed.replaceAll("\\s+", "")
        );
    }

    @Test
    void shouldRejectEmptyMarkdown() {

        assertThrows(
                IllegalArgumentException.class,
                () -> markdownSectionChunker.split("")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> markdownSectionChunker.split(null)
        );
    }

    @Test
    void shouldGenerateStructuredChunksWithMetadata() throws IOException {

        List<Path> documents =
                markdownDocumentReader.discoverDocuments();

        assertEquals(4, documents.size());

        for (Path document : documents) {

            String markdown =
                    markdownDocumentReader.readDocument(document);

            List<MarkdownChunk> chunks =
                    markdownSectionChunker.splitWithMetadata(
                            document.getFileName().toString(),
                            markdown
                    );

            assertEquals(7, chunks.size());

            String expectedTitle = markdown.lines()
                    .findFirst()
                    .orElse("")
                    .replaceFirst("^#\\s+", "")
                    .trim();

            for (int i = 0; i < chunks.size(); i++) {

                MarkdownChunk chunk = chunks.get(i);

                assertEquals(
                        document.getFileName().toString(),
                        chunk.filename()
                );

                assertEquals(expectedTitle, chunk.documentTitle());

                assertEquals(i, chunk.chunkIndex());

                assertFalse(chunk.content().isBlank());
            }

            assertEquals(
                    "Document Metadata",
                    chunks.get(0).sectionHeading()
            );

            assertEquals("Symptoms", chunks.get(1).sectionHeading());
            assertEquals("Likely causes", chunks.get(2).sectionHeading());
            assertEquals("Diagnosis", chunks.get(3).sectionHeading());
            assertEquals("Resolution", chunks.get(4).sectionHeading());
            assertEquals("Verification", chunks.get(5).sectionHeading());
            assertEquals("Prevention", chunks.get(6).sectionHeading());

            System.out.println("--------------------------------");
            System.out.println("Document: " + document.getFileName());

            for (MarkdownChunk chunk : chunks) {
                System.out.printf(
                        "Chunk %d | Section: %s | Characters: %d%n",
                        chunk.chunkIndex(),
                        chunk.sectionHeading(),
                        chunk.content().length()
                );
            }
        }
    }
    @Test
    void shouldSplitOversizedSectionsAndPreserveMetadata() throws IOException {

        List<Path> documents =
                markdownDocumentReader.discoverDocuments();

        for (Path document : documents) {

            String markdown =
                    markdownDocumentReader.readDocument(document);

            // Stage 1: Section-aware chunking
            List<MarkdownChunk> sections =
                    markdownSectionChunker.splitWithMetadata(
                            document.getFileName().toString(),
                            markdown
                    );

            // Stage 2: Size-aware chunking
            List<MarkdownChunk> finalChunks =
                    markdownSizeAwareChunker.split(sections);

            assertTrue(finalChunks.size() >= sections.size());

            for (int i = 0; i < finalChunks.size(); i++) {

                MarkdownChunk chunk = finalChunks.get(i);

                assertEquals(i, chunk.chunkIndex());

                assertEquals(
                        document.getFileName().toString(),
                        chunk.filename()
                );

                assertEquals(
                        sections.getFirst().documentTitle(),
                        chunk.documentTitle()
                );

                assertFalse(chunk.content().isBlank());
            }

            System.out.println("--------------------------------");
            System.out.println("Document: " + document.getFileName());
            System.out.println("Section chunks: " + sections.size());
            System.out.println("Final chunks: " + finalChunks.size());

            for (MarkdownChunk chunk : finalChunks) {
                System.out.printf(
                        "Chunk %d | Section: %s | Characters: %d%n",
                        chunk.chunkIndex(),
                        chunk.sectionHeading(),
                        chunk.content().length()
                );
            }
        }
    }

    @Test
    void shouldPreserveContentAfterSizeAwareSplitting() throws IOException {

        List<Path> documents =
                markdownDocumentReader.discoverDocuments();

        for (Path document : documents) {

            String markdown =
                    markdownDocumentReader.readDocument(document);

            List<MarkdownChunk> sections =
                    markdownSectionChunker.splitWithMetadata(
                            document.getFileName().toString(),
                            markdown
                    );

            List<MarkdownChunk> finalChunks =
                    markdownSizeAwareChunker.split(sections);

            String originalContent = sections.stream()
                    .map(MarkdownChunk::content)
                    .collect(java.util.stream.Collectors.joining());

            StringBuilder reconstructed = new StringBuilder();

            for (MarkdownChunk section : sections) {

                List<MarkdownChunk> sectionChunks = finalChunks.stream()
                        .filter(chunk ->
                                chunk.sectionHeading().equals(section.sectionHeading()))
                        .toList();

                assertFalse(sectionChunks.isEmpty());

                StringBuilder sectionContent = getStringBuilder(sectionChunks);

                assertEquals(
                        section.content().replaceAll("\\s+", ""),
                        sectionContent.toString().replaceAll("\\s+", ""),
                        "Content mismatch in section: " + section.sectionHeading()
                );

                reconstructed.append(sectionContent);
            }

            assertEquals(
                    originalContent.replaceAll("\\s+", ""),
                    reconstructed.toString().replaceAll("\\s+", ""),
                    "Content mismatch for " + document.getFileName()
            );
        }
    }

    private static @NonNull StringBuilder getStringBuilder(List<MarkdownChunk> sectionChunks) {
        StringBuilder sectionContent = new StringBuilder(
                sectionChunks.getFirst().content()
        );

        for (int i = 1; i < sectionChunks.size(); i++) {

            String previous = sectionChunks.get(i - 1).content();
            String current = sectionChunks.get(i).content();

            int overlapLength = Math.min(150, previous.length());

            String expectedOverlap = previous.substring(
                    previous.length() - overlapLength
            );

            if (current.startsWith(expectedOverlap)) {
                sectionContent.append(current.substring(overlapLength));
            } else {
                sectionContent.append(current);
            }
        }
        return sectionContent;
    }

    @Test
    void shouldHandleSingleOversizedMarkdownBlock() {

        String longParagraph = "Database connection timeout. ".repeat(50);

        MarkdownChunk section = new MarkdownChunk(
                "test-runbook.md",
                "Database Troubleshooting",
                "Diagnosis",
                0,
                "## Diagnosis\n" + longParagraph
        );

        List<MarkdownChunk> result =
                markdownSizeAwareChunker.split(List.of(section));

        assertTrue(result.size() > 1,
                "Oversized section should produce multiple chunks");

        for (MarkdownChunk chunk : result) {
            assertTrue(
                    chunk.content().length() <= 1000,
                    "Chunk exceeds maximum size: "
                            + chunk.content().length()
            );
        }
    }

    @Test
    void shouldCreateOverlappingChunks() {

        String content =
                "## Resolution\n"
                        + "1. " + "A".repeat(400) + "\n"
                        + "2. " + "B".repeat(400) + "\n"
                        + "3. " + "C".repeat(300) + "\n";

        MarkdownChunk section = new MarkdownChunk(
                "test-runbook.md",
                "Payment Troubleshooting",
                "Resolution",
                0,
                content
        );

        List<MarkdownChunk> chunks =
                markdownSizeAwareChunker.split(List.of(section));

        assertEquals(2, chunks.size());

        MarkdownChunk first = chunks.get(0);
        MarkdownChunk second = chunks.get(1);

        // Check maximum chunk size
        assertTrue(first.content().length() <= 1000);
        assertTrue(second.content().length() <= 1000);

        // Verify 150-character overlap
        String expectedOverlap = first.content()
                .substring(first.content().length() - 150);

        assertTrue(
                second.content().startsWith(expectedOverlap),
                "Second chunk should start with 150 characters from first chunk"
        );

        // Verify sequential indexes
        assertEquals(0, first.chunkIndex());
        assertEquals(1, second.chunkIndex());

        System.out.println("First chunk: " + first.content().length());
        System.out.println("Second chunk: " + second.content().length());
        System.out.println("Overlap verified: 150 characters");
    }

    @Test
    void shouldOverlapWhenSingleBlockExceedsMaximumSize() {

        String content = "X".repeat(1500);

        MarkdownChunk section = new MarkdownChunk(
                "test-runbook.md",
                "Database Troubleshooting",
                "Diagnosis",
                0,
                content
        );

        List<MarkdownChunk> chunks =
                markdownSizeAwareChunker.split(List.of(section));

        assertEquals(2, chunks.size());

        MarkdownChunk first = chunks.get(0);
        MarkdownChunk second = chunks.get(1);

        assertTrue(first.content().length() <= 1000);
        assertTrue(second.content().length() <= 1000);

        assertEquals(1000, first.content().length());
        assertEquals(650, second.content().length());

        String expectedOverlap =
                first.content().substring(850);

        assertTrue(
                second.content().startsWith(expectedOverlap),
                "Oversized-block fallback should preserve 150-character overlap"
        );
    }

    @Test
    void shouldHandlePartiallyFilledChunkBeforeOversizedBlock() {

        String content =
                "## Diagnosis\n"
                        + "1. " + "A".repeat(300) + "\n"
                        + "2. " + "B".repeat(1500) + "\n";

        MarkdownChunk section = new MarkdownChunk(
                "test-runbook.md",
                "Database Troubleshooting",
                "Diagnosis",
                0,
                content
        );

        List<MarkdownChunk> chunks =
                markdownSizeAwareChunker.split(List.of(section));

        assertTrue(chunks.size() > 1);

        for (MarkdownChunk chunk : chunks) {
            assertFalse(chunk.content().isBlank());

            assertTrue(
                    chunk.content().length() <= 1000,
                    "Oversized chunk at index "
                            + chunk.chunkIndex()
            );
        }

        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).chunkIndex());
        }

        System.out.println("Total chunks: " + chunks.size());

        for (MarkdownChunk chunk : chunks) {
            System.out.println(
                    "Chunk " + chunk.chunkIndex()
                            + ": " + chunk.content().length()
                            + " characters"
            );
        }
        String combined = chunks.stream()
                .map(MarkdownChunk::content)
                .collect(java.util.stream.Collectors.joining());

        assertTrue(
                combined.contains("A".repeat(300)),
                "First troubleshooting step was lost"
        );

        assertTrue(
                combined.contains("B".repeat(1000)),
                "Oversized block was not preserved continuously"
        );

        assertTrue(
                chunks.get(2).content().endsWith("B".repeat(1000))
                        || chunks.get(2).content().contains("B".repeat(500)),
                "Final portion of oversized block is missing"
        );
        for (int i = 1; i < chunks.size(); i++) {

            String previous = chunks.get(i - 1).content();
            String current = chunks.get(i).content();

            String expectedOverlap = previous.substring(
                    Math.max(0, previous.length() - 150)
            );

            boolean hasOverlap = current.startsWith(expectedOverlap);

            System.out.println(
                    "Chunk " + (i - 1) + " -> Chunk " + i
                            + " | 150-character overlap: " + hasOverlap
            );

            assertTrue(
                    hasOverlap,
                    "Missing expected overlap between chunks "
                            + (i - 1) + " and " + i
            );
        }
    }

    @Test
    void shouldNotCreateOverlapOnlyChunk() {

        String content =
                "## Diagnosis\n"
                        + "1. " + "A".repeat(896) + "\n"
                        + "2. " + "B".repeat(996) + "\n";

        MarkdownChunk section = new MarkdownChunk(
                "test-runbook.md",
                "Database Troubleshooting",
                "Diagnosis",
                0,
                content
        );

        List<MarkdownChunk> chunks =
                markdownSizeAwareChunker.split(List.of(section));

        assertEquals(
                2,
                chunks.size(),
                "Expected two meaningful chunks without an overlap-only chunk"
        );

        for (MarkdownChunk chunk : chunks) {
            assertTrue(chunk.content().length() <= 1000);
        }

        assertTrue(chunks.get(0).content().contains("A".repeat(896)));
        assertTrue(chunks.get(1).content().contains("B".repeat(996)));
    }

    @Test
    void shouldNotCreateOverlapOnlyChunkAfterFlush() {

        String content =
                "## Diagnosis\n"
                        + "1. " + "A".repeat(896) + "\n"
                        + "2. " + "B".repeat(996) + "\n"
                        + "3. " + "C".repeat(996) + "\n";

        MarkdownChunk section = new MarkdownChunk(
                "test-runbook.md",
                "Database Troubleshooting",
                "Diagnosis",
                0,
                content
        );

        List<MarkdownChunk> chunks =
                markdownSizeAwareChunker.split(List.of(section));

        assertEquals(
                3,
                chunks.size(),
                "Each list item should produce a meaningful chunk"
        );

        for (MarkdownChunk chunk : chunks) {
            assertFalse(chunk.content().isBlank());
            assertTrue(chunk.content().length() <= 1000);
        }

        assertTrue(chunks.get(2).content().contains("C".repeat(996)));
    }

    @Test
    void shouldNotGenerateOverlapOnlyChunkAfterOversizedBlock() {

        String content =
                "## Diagnosis\n"
                        + "1. " + "A".repeat(1500) + "\n"
                        + "2. " + "B".repeat(996) + "\n";

        MarkdownChunk section = new MarkdownChunk(
                "test-runbook.md",
                "Database Troubleshooting",
                "Diagnosis",
                0,
                content
        );

        List<MarkdownChunk> chunks =
                markdownSizeAwareChunker.split(List.of(section));

        for (MarkdownChunk chunk : chunks) {
            assertFalse(chunk.content().isBlank());
            assertTrue(chunk.content().length() <= 1000);
        }

        assertTrue(
                chunks.stream().anyMatch(
                        chunk -> chunk.content().contains("B".repeat(996))
                ),
                "The second list item must be preserved"
        );

        System.out.println("Total chunks: " + chunks.size());

        for (MarkdownChunk chunk : chunks) {
            System.out.println(
                    "Chunk " + chunk.chunkIndex()
                            + ": " + chunk.content().length()
                            + " characters"
            );
        }
    }

    @Test
    void shouldNotCreateHeadingOnlyChunk() {

        String content =
                "## Diagnosis\n"
                        + "1. " + "A".repeat(1500) + "\n"
                        + "2. " + "B".repeat(996) + "\n";

        MarkdownChunk section = new MarkdownChunk(
                "test-runbook.md",
                "Database Troubleshooting",
                "Diagnosis",
                0,
                content
        );

        List<MarkdownChunk> chunks =
                markdownSizeAwareChunker.split(List.of(section));

        assertFalse(
                chunks.get(0).content().trim().equals("## Diagnosis"),
                "The first chunk must contain more than just the heading"
        );
    }
}