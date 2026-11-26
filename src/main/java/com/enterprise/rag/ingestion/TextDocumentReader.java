package com.enterprise.rag.ingestion;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class TextDocumentReader {
    public String readDocument(Path filePath) throws IOException {

        if (!Files.isRegularFile(filePath)) {
            throw new IOException(
                    "Document not found: " + filePath
            );
        }

        return Files.readString(filePath);
    }
}
