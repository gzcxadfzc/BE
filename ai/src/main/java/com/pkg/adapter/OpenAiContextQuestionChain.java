package com.pkg.adapter;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class OpenAiContextQuestionChain {

    private final OpenAiContextQuestionGenerator generator;

    public OpenAiContextQuestionChain(OpenAiContextQuestionGenerator generator) {
        this.generator = generator;
    }

    @Async("open-ai-api")
    public CompletableFuture<ContextQuestion> asyncGetContextQuestion(BookProgressInfo bookProgressInfo) {
        return CompletableFuture.completedFuture(generator.operate(bookProgressInfo));
    }
}
