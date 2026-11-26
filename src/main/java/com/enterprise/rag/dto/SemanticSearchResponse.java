package com.enterprise.rag.dto;

import java.util.UUID;

public record SemanticSearchResponse(
        UUID id,
        UUID documentId,
        int chunkIndex,
        String content,
        double distance,
        String filename,
        String documentTitle,
        String sectionHeading
) {

}
