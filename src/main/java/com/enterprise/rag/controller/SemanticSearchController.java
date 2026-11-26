package com.enterprise.rag.controller;

import com.enterprise.rag.dto.SemanticSearchResponse;
import com.enterprise.rag.service.SemanticSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/search")
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;
    public SemanticSearchController(SemanticSearchService semanticSearchService) {
        this.semanticSearchService = semanticSearchService;
    }
    @GetMapping
    public List<SemanticSearchResponse> search(
            @RequestParam
            @NotBlank(message = "Question cannot be blank")
            String query,
            @RequestParam(defaultValue = "3")
            @Min(value = 1,message = "topk must be atleast 1")
            @Max(value = 20,message = "topK cannot exceed 20")
            int topK) {
        log.info("Searching for query {}", query);
        return semanticSearchService.search(query, topK);
    }
}
