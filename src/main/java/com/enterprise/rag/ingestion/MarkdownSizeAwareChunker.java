package com.enterprise.rag.ingestion;

import com.enterprise.rag.dto.MarkdownChunk;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
public class MarkdownSizeAwareChunker {

    private final int maxCharacters;
    private final int overlapCharacters;
    public MarkdownSizeAwareChunker(@Value("${rag.chunking.max-characters}") int maxCharacters,
                                    @Value("${rag.chunking.overlap-characters}") int overlapCharacters) {
        if (maxCharacters <= 0) {
            throw new IllegalArgumentException("max characters must be greater than 0");
        }
        if (overlapCharacters <= 0 || overlapCharacters >= maxCharacters) {
            throw new IllegalArgumentException("overlap characters must be between 0 and maxCharacters -1");
        }
        this.maxCharacters = maxCharacters;
        this.overlapCharacters = overlapCharacters;
    }
    public List<MarkdownChunk> split(List<MarkdownChunk> sections) {

        List<MarkdownChunk> result = new ArrayList<>();
        int nextChunkIndex = 0;

        for (MarkdownChunk section : sections) {

            String content = section.content();

            if (content.length() <= maxCharacters) {
                result.add(new MarkdownChunk(
                        section.filename(),
                        section.documentTitle(),
                        section.sectionHeading(),
                        nextChunkIndex++,
                        content
                ));
                continue;
            }

            String[] blocks = content.split("(?m)(?=^\\d+\\.\\s|^-\\s)");

            StringBuilder current = new StringBuilder();

            for (String block : blocks) {

                boolean headingOnly =
                        current.toString().trim().equals(
                                "## " + section.sectionHeading()
                        );

                if (!current.isEmpty()
                        && current.length() + block.length() > maxCharacters
                        && !(headingOnly && block.length() > maxCharacters)) {

                    String previousContent = current.toString().trim();

                    result.add(new MarkdownChunk(
                            section.filename(),
                            section.documentTitle(),
                            section.sectionHeading(),
                            nextChunkIndex++,
                            previousContent
                    ));

                    current.setLength(0);

                    // Retain the previous chunk's context for the next chunk.
                    String overlap = getOverlapText(previousContent);

                    // Only add overlap if the next block will still fit.
                    if (block.length() > maxCharacters) {

                        // Oversized blocks are split using remainingCapacity.
                        // We can carry overlap before processing the block.
                        current.append(overlap);

                    } else if (overlap.length() + block.length() <= maxCharacters) {

                        current.append(overlap);
                    }
                }

                if (block.length() > maxCharacters) {

                    int start = 0;

                    while (start < block.length()) {

                        int remainingCapacity =
                                maxCharacters - current.length();

                        if (remainingCapacity == 0) {

                            String previousContent = current.toString();

                            result.add(new MarkdownChunk(
                                    section.filename(),
                                    section.documentTitle(),
                                    section.sectionHeading(),
                                    nextChunkIndex++,
                                    previousContent
                            ));

                            current.setLength(0);

                            // Carry the final 150 characters into the next chunk.
                            String overlap = getOverlapText(previousContent);
                            current.append(overlap);

                            // Recalculate capacity after adding overlap.
                            remainingCapacity = maxCharacters - current.length();
                        }

                        int end = Math.min(
                                start + remainingCapacity,
                                block.length()
                        );

                        current.append(block, start, end);
                        start = end;
                    }

                } else {
                    current.append(block);
                }
            }

            if (!current.isEmpty()) {
                result.add(new MarkdownChunk(
                        section.filename(),
                        section.documentTitle(),
                        section.sectionHeading(),
                        nextChunkIndex++,
                        current.toString().trim()
                ));
            }
        }

        return result;
    }

    private String getOverlapText(String previousChunk) {

        if (previousChunk == null || previousChunk.isEmpty()
                || overlapCharacters == 0) {
            return "";
        }

        int start = Math.max(
                0,
                previousChunk.length() - overlapCharacters
        );

        return previousChunk.substring(start);
    }


    private MarkdownChunk createChunk(
            MarkdownChunk section,
            int chunkIndex,
            String content,
            String previousContent
    ) {

        String overlap = getOverlapText(previousContent);

        int available = maxCharacters - content.length();

        if (available <= 0) {
            overlap = "";
        } else if (overlap.length() > available) {
            overlap = overlap.substring(
                    overlap.length() - available
            );
        }

        return new MarkdownChunk(
                section.filename(),
                section.documentTitle(),
                section.sectionHeading(),
                chunkIndex,
                overlap + content
        );
    }

}
