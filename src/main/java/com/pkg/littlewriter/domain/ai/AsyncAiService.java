package com.pkg.littlewriter.domain.ai;

import com.pkg.littlewriter.domain.ai.commons.Jsonable;
import com.pkg.littlewriter.domain.ai.exceptions.AiException;
import com.pkg.littlewriter.domain.ai.input.AiJsonInput;
import com.pkg.littlewriter.domain.ai.input.AiTextInput;
import com.pkg.littlewriter.domain.ai.response.AiImageResponse;
import com.pkg.littlewriter.domain.ai.response.AiJsonResponse;
import com.pkg.littlewriter.domain.ai.response.ContextAndQuestionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncAiService {
    private final AiImageGenerator aiImageGenerator;
    private final ContextQuestionGenerator contextQuestionGenerator;

    @Autowired
    public AsyncAiService(AiImageGenerator aiImageGenerator, ContextQuestionGenerator contextQuestionGenerator) {
        this.aiImageGenerator = aiImageGenerator;
        this.contextQuestionGenerator = contextQuestionGenerator;
    }

    @Async
    public CompletableFuture<AiImageResponse> asyncGenerateImageFrom(Jsonable<?> jsonable) throws AiException {
        return CompletableFuture.completedFuture(aiImageGenerator.getResponseFrom(new AiTextInput(jsonable.getJsonString())));
    }

    @Async
    public CompletableFuture<AiJsonResponse<ContextAndQuestionDto>> asyncGenerateContextAndQuestionFrom(Jsonable<?> jsonable) throws AiException {
        return CompletableFuture.completedFuture(contextQuestionGenerator.getResponseFrom(new AiJsonInput<>(jsonable)));
    }
}
