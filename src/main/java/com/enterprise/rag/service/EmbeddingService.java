package com.enterprise.rag.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    public float[] generateEmbedding(String text) {

        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(
                    "Text cannot be null or empty"
            );
        }
        return embeddingModel.embed(text);
    }
}
