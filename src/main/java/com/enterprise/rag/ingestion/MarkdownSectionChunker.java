package com.enterprise.rag.ingestion;

import com.enterprise.rag.dto.MarkdownChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MarkdownSectionChunker {

    public List<String> split(String markdown){
        if(markdown == null || markdown.isBlank()){
            throw new IllegalArgumentException("makrdown cannot be null or empty");
        }

        List<String> chunks  = new ArrayList<>();
        StringBuilder currentSection = new StringBuilder();

        String[] lines = markdown.split("\\R");

        for(String line : lines){

            if(line.matches("^##\\s+.+")){
                if(!currentSection.isEmpty()){
                    chunks.add(currentSection.toString().trim());
                    currentSection.setLength(0);
                }
            }
            currentSection.append(line).append("\n");
        }
        if (!currentSection.isEmpty()) {
            chunks.add(currentSection.toString().trim());
        }

        return chunks;
    }

    public List<MarkdownChunk> splitWithMetadata(
            String filename,
            String markdown
    ) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Filename cannot be empty"
            );
        }

        List<String> sections = split(markdown);

        String documentTitle = sections.getFirst()
                .lines()
                .findFirst()
                .orElse("")
                .replaceFirst("^#\\s+", "")
                .trim();

        List<MarkdownChunk> chunks = new ArrayList<>();

        for (int i = 0; i < sections.size(); i++) {

            String section = sections.get(i);

            String sectionHeading = i == 0
                    ? "Document Metadata"
                    : section.lines()
                      .findFirst()
                      .orElse("")
                      .replaceFirst("^##\\s+", "")
                      .trim();

            chunks.add(new MarkdownChunk(
                    filename,
                    documentTitle,
                    sectionHeading,
                    i,
                    section
            ));
        }

        return chunks;
    }
}
