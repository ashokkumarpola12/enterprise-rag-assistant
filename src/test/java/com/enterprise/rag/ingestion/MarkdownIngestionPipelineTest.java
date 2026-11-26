package com.enterprise.rag.ingestion;

import com.enterprise.rag.dto.MarkdownChunk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MarkdownIngestionPipelineTest {

    @Autowired
    private MarkdownIngestionPipeline pipeline;

    @Test
    void shouldPrepareChunksFromAllRunbooks() throws IOException {

        List<MarkdownChunk> chunks = pipeline.prepareChunks();

        assertFalse(chunks.isEmpty());

        Map<String, List<MarkdownChunk>> chunksByFile =
                chunks.stream()
                        .collect(Collectors.groupingBy(
                                MarkdownChunk::filename
                        ));

        assertEquals(4, chunksByFile.size());

        for (Map.Entry<String, List<MarkdownChunk>> entry
                : chunksByFile.entrySet()) {

            List<MarkdownChunk> documentChunks = entry.getValue();

            assertFalse(documentChunks.isEmpty());

            for (int i = 0; i < documentChunks.size(); i++) {

                MarkdownChunk chunk = documentChunks.get(i);

                assertEquals(i, chunk.chunkIndex());
                assertFalse(chunk.content().isBlank());
                assertTrue(chunk.content().length() <= 1000);

                assertFalse(chunk.documentTitle().isBlank());
                assertFalse(chunk.sectionHeading().isBlank());
            }

            System.out.println(
                    entry.getKey() + " -> "
                    + documentChunks.size() + " chunks"
            );
        }
    }

    @Test
    void shouldPreserveMetadataAcrossPipeline() throws IOException {

        List<MarkdownChunk> chunks = pipeline.prepareChunks();

        for (MarkdownChunk chunk : chunks) {

            System.out.printf(
                    "File: %s | Index: %d | Section: %s | Size: %d%n",
                    chunk.filename(),
                    chunk.chunkIndex(),
                    chunk.sectionHeading(),
                    chunk.content().length()
            );

            assertNotNull(chunk.filename());
            assertFalse(chunk.filename().isBlank());

            assertNotNull(chunk.documentTitle());
            assertFalse(chunk.documentTitle().isBlank());

            assertNotNull(chunk.sectionHeading());
            assertFalse(chunk.sectionHeading().isBlank());

            assertNotNull(chunk.content());
            assertFalse(chunk.content().isBlank());
        }
    }
}