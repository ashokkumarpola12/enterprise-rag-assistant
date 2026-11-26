package com.enterprise.rag.ingestion;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Component
public class MarkdownDocumentReader {

    private final Path knowledgeBaseDirectory;

    public MarkdownDocumentReader(@Value("${rag.ingestion.directory}") String directory) {
        this.knowledgeBaseDirectory = Path.of(directory);
    }

    public List<Path> discoverDocuments() throws IOException {

        if(!Files.isDirectory(knowledgeBaseDirectory)) {
            throw new IOException("Knowledge base directory is not a directory" + knowledgeBaseDirectory);
        }
        try(Stream<Path> paths = Files.list(knowledgeBaseDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .toLowerCase()
                            .endsWith(".md"))
                    .sorted()
                    .toList();
        }
    }

    public String readDocument(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
