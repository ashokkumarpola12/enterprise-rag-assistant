package com.enterprise.rag.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MarkdownDocumentReaderTest {

    @Autowired
    private MarkdownDocumentReader markdownDocumentReader;

    @Test
    void shouldDiscoverAllMarkdownRunbooks() throws IOException {

        List<Path> documents =
                markdownDocumentReader.discoverDocuments();

        assertEquals(4, documents.size());

        documents.forEach(path ->
                System.out.println("Discovered: " + path.getFileName())
        );
    }

    @Test
    void shouldReadMarkdownDocumentContent() throws IOException {

        List<Path> documents =
                markdownDocumentReader.discoverDocuments();

        Path firstDocument = documents.get(0);

        String content =
                markdownDocumentReader.readDocument(firstDocument);

        assertFalse(content.isBlank());

        assertTrue(content.contains(
                "Payment Service High Latency"
        ));

        System.out.println("Filename: " + firstDocument.getFileName());
        System.out.println("Content length: " + content.length());
        System.out.println("Preview: " +
                content.substring(0, Math.min(150, content.length())));
    }
}