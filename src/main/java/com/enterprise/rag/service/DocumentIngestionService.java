
package com.enterprise.rag.service;

import com.enterprise.rag.dto.MarkdownChunk;
import com.enterprise.rag.ingestion.TextChunker;
import com.enterprise.rag.ingestion.TextDocumentReader;
import com.enterprise.rag.repository.DocumentRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private final TextDocumentReader documentReader;
    private final TextChunker textChunker;
    private final DocumentRepository documentRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public DocumentIngestionService(
            TextDocumentReader documentReader,
            TextChunker textChunker,
            DocumentRepository documentRepository,
            EmbeddingService embeddingService,
            ObjectMapper objectMapper
    ) {
        this.documentReader = documentReader;
        this.textChunker = textChunker;
        this.documentRepository = documentRepository;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public UUID ingestDocument(Path filePath) throws IOException {

        // Step 1: Read document content
        String content = documentReader.readDocument(filePath);

        // Step 2: Split content into chunks
        List<String> chunks = textChunker.split(content, 200, 50);

        // Step 3: Generate a unique document ID
        UUID documentId = UUID.randomUUID();

        // Step 4: Save document metadata
        documentRepository.saveDocument(
                documentId,
                filePath.getFileName().toString(),
                "text/plain"
        );

        // Step 5: Save each chunk and generate embeddings
        for (int i = 0; i < chunks.size(); i++) {
            UUID chunkId = UUID.randomUUID();
            documentRepository.saveChunk(
                    chunkId,
                    documentId,
                    i,
                    chunks.get(i),
                    null
            );

            float[] embedding  = embeddingService.generateEmbedding(chunks.get(i));

            documentRepository.updateChunkEmbedding(chunkId,embedding);
        }

        return documentId;
    }

    @Transactional(rollbackFor = Exception.class)
    public UUID ingestMarkdownChunks(List<MarkdownChunk> chunks)
            throws JacksonException {

        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Chunks cannot be empty");
        }

        String filename = chunks.getFirst().filename();

        boolean alreadyExists =
                documentRepository.existsByFilenameAndContentType(
                        filename,
                        "text/markdown"
                );

        if (alreadyExists) {
            throw new IllegalStateException(
                    "Markdown document already exists: " + filename
            );
        }

        UUID documentId = UUID.randomUUID();

        documentRepository.saveDocument(
                documentId,
                filename,
                "text/markdown"
        );

        for (MarkdownChunk chunk : chunks) {

            if (!filename.equals(chunk.filename())) {
                throw new IllegalArgumentException(
                        "All chunks must belong to the same document"
                );
            }

            Map<String, Object> metadata = Map.of(
                    "filename", chunk.filename(),
                    "documentTitle", chunk.documentTitle(),
                    "sectionHeading", chunk.sectionHeading(),
                    "chunkIndex", chunk.chunkIndex()
            );

            String metadataJson =
                    objectMapper.writeValueAsString(metadata);

            documentRepository.saveChunk(
                    UUID.randomUUID(),
                    documentId,
                    chunk.chunkIndex(),
                    chunk.content(),
                    metadataJson
            );
        }

        return documentId;
    }

    //insert the generated embeddings for the chunks in postgresql
    public int generateEmbeddingsForDocument(UUID documentId) {
        //retrieve chunks without embedddings
        List<Map<String,Object>> chunks = documentRepository.findChunksWithoutEmbeddings(documentId);
        int processedCount = 0;
        //process each chunk
        for (Map<String, Object> chunk : chunks) {
            UUID chunkId = (UUID) chunk.get("id");
            String content = (String) chunk.get("content");

            //Generate embedding using ollama
            float[] embedding = embeddingService.generateEmbedding(content);
            //store embedding in postgresql
            documentRepository.updateChunkEmbedding(chunkId, embedding);
            processedCount++;
            System.out.println("Generated embedding for chunk: " + chunkId);
        }
        //Status updates to COMPLETED when all chunks have embeddings.
        if (documentRepository.areAllChunksEmbedded(documentId)) {

            int updatedRows = documentRepository.updateDocumentStatus(
                    documentId,
                    "COMPLETED"
            );

            if (updatedRows != 1) {
                throw new IllegalStateException(
                        "Failed to update document status: " + documentId
                );
            }
        }
        return processedCount;
    }
}
