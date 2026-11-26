package com.enterprise.rag.service;

import com.enterprise.rag.dto.SemanticSearchResponse;
import com.enterprise.rag.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SemanticSearchService {
    private final EmbeddingService embeddingService;
    private final DocumentRepository documentRepository;
    public SemanticSearchService(EmbeddingService embeddingService,
                                 DocumentRepository documentRepository) {
        this.embeddingService = embeddingService;
        this.documentRepository = documentRepository;
    }
    public List<SemanticSearchResponse> search(String question, int topK){
        if(question == null || question.isBlank()){
            throw new IllegalArgumentException("Question cannot be null or blank");
        }
        if(topK < 1){
            throw new IllegalArgumentException("Top K cannot be less than 1");
        }
        //convert question into embedding
        float[] queryEmbedding = embeddingService.generateEmbedding(question);

        //retrieve semantically similar chunks
        return documentRepository.searchSimilarChunks(queryEmbedding,topK);

    }
}
