package com.enterprise.rag.ingestion;

import com.enterprise.rag.dto.MarkdownChunk;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class MarkdownIngestionPipeline {

    private final MarkdownDocumentReader documentReader;
    private final MarkdownSectionChunker sectionChunker;
    private final MarkdownSizeAwareChunker sizeAwareChunker;

    public MarkdownIngestionPipeline(
            MarkdownDocumentReader documentReader,
            MarkdownSectionChunker sectionChunker,
            MarkdownSizeAwareChunker sizeAwareChunker) {

        this.documentReader = documentReader;
        this.sectionChunker = sectionChunker;
        this.sizeAwareChunker = sizeAwareChunker;
    }

    public List<MarkdownChunk> prepareChunks() throws IOException {

        List<MarkdownChunk> allChunks = new ArrayList<>();

        for (Path path : documentReader.discoverDocuments()) {

            String markdown = documentReader.readDocument(path);

            List<MarkdownChunk> sections =
                    sectionChunker.splitWithMetadata(
                            path.getFileName().toString(),
                            markdown
                    );

            List<MarkdownChunk> chunks =
                    sizeAwareChunker.split(sections);

            allChunks.addAll(chunks);
        }

        return allChunks;
    }
}