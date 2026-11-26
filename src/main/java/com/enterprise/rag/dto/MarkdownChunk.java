package com.enterprise.rag.dto;

public record MarkdownChunk(
        String filename,
        String documentTitle,
        String sectionHeading,
        int chunkIndex,
        String content
) {
}