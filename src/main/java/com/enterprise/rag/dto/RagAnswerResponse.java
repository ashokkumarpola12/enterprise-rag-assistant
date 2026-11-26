package com.enterprise.rag.dto;

import java.util.List;

public record RagAnswerResponse(
        String question,
        String answer,
        List<RagSourceResponse>  sources
) {
}
