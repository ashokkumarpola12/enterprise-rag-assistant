package com.enterprise.rag.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class AnswerGenerationService {

    private final ChatModel chatModel;
    public AnswerGenerationService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String generateAnswer(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Question cannot be null or blank"
            );
        }
        return chatModel.call(question);
    }
}
