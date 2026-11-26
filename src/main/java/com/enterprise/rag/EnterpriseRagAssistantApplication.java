package com.enterprise.rag;

import com.enterprise.rag.service.AnswerGenerationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EnterpriseRagAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseRagAssistantApplication.class, args);
    }

}
