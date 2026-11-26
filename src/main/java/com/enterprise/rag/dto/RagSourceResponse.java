package com.enterprise.rag.dto;

import java.util.UUID;

public record RagSourceResponse(
        UUID documentId,
        int chunkIndex,
        double distance,
        String filename,
        String documentTitle,
        String sectionHeading
) {
}
