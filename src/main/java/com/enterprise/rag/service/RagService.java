package com.enterprise.rag.service;

import com.enterprise.rag.dto.RagAnswerResponse;
import com.enterprise.rag.dto.RagSourceResponse;
import com.enterprise.rag.dto.SemanticSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final SemanticSearchService semanticSearchService;
    private final AnswerGenerationService answerGenerationService;
    @Value("${rag.retrieval.max-distance}")
    private double maxDistance;


    public RagService(SemanticSearchService semanticSearchService, AnswerGenerationService answerGenerationService) {
        this.semanticSearchService = semanticSearchService;
        this.answerGenerationService = answerGenerationService;
    }

    public RagAnswerResponse answerQuestion(String question){

        if(question == null || question.isBlank()){
            throw new IllegalArgumentException(
                    "Question cannot be null or blank"
            );
        }
        //Step 1: Retrieve relevant document chunks
        List<SemanticSearchResponse> chunks = semanticSearchService.search(question,3)
                .stream()
                        .filter(chunk -> chunk.distance() <= maxDistance)
                .toList();
        if(chunks.isEmpty()){
            return new RagAnswerResponse(question,"I couldn't find relevant information in the knowledge base.",
                    List.of()
            );
        }

        //Step 2:Build context from retrieved chunks
        String context = chunks.stream()
                .map(SemanticSearchResponse::content)
                .collect(Collectors.joining("\n\n"));

        //Step 3: Construct the generated prompt
        String prompt = """
                You are an enterprise knowledge assistant.

                Answer the user's question using only the
                provided context.

                If the context does not contain enough
                information, say that you do not have
                enough information to answer.

                Context:
                %s

                Question:
                %s
                """.formatted(context, question);

        //Step 4: Generate answer using LLM
        String answer = answerGenerationService.generateAnswer(prompt);
        List<RagSourceResponse> sources = chunks.stream()
                .map(chunk -> new RagSourceResponse(
                        chunk.documentId(),
                        chunk.chunkIndex(),
                        chunk.distance(),
                        chunk.filename(),
                        chunk.documentTitle(),
                        chunk.sectionHeading()
                ))
                .toList();
        return new  RagAnswerResponse(question, answer, sources);
    }
}
