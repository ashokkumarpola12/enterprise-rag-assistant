package com.enterprise.rag.controller;

import com.enterprise.rag.dto.RagAnswerResponse;
import com.enterprise.rag.service.RagService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
@Validated
public class RagController {

    private final RagService ragService;
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/ask")
    public RagAnswerResponse askQuestion(@RequestParam @NotBlank(message="Question cannot be blank") String question) {
        return ragService.answerQuestion(question);
    }
}
