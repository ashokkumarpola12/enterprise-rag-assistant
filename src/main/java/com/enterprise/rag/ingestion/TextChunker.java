
package com.enterprise.rag.ingestion;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    public List<String> split(
            String text,
            int chunkSize,
            int overlap
    ) {

        if (text == null || text.isBlank()) {
            return List.of();
        }

        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "chunkSize must be positive and overlap must be between 0 and chunkSize - 1"
            );
        }

        List<String> chunks = new ArrayList<>();

        int start = 0;

        while (start < text.length()) {

            int end = Math.min(start + chunkSize, text.length());

            String chunk = text.substring(start, end);

            chunks.add(chunk);

            if (end == text.length()) {
                break;
            }

            start = end - overlap;
        }

        return chunks;
    }
}
