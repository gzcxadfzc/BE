//package com.pkg.littlewriter.domain.external.ai;
//
//import com.pkg.littlewriter.domain.external.ai.exceptions.AiException;
//import com.pkg.littlewriter.domain.external.ai.input.GenerateContextQuestionInputDto;
//import com.pkg.littlewriter.domain.external.ai.input.GenerateImageInputDto;
//import com.pkg.littlewriter.domain.external.ai.response.GenerateContextQuestionResponseDto;
//import com.pkg.littlewriter.domain.external.ai.response.GenerateImageResponseDto;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.CompletableFuture;
//
//@Service
//public class AsyncAiService {
//    private final AiImageGenerator aiImageGenerator;
//    private final ContextQuestionGenerator contextQuestionGenerator;
//
//    @Autowired
//    public AsyncAiService(AiImageGenerator aiImageGenerator, ContextQuestionGenerator contextQuestionGenerator) {
//        this.aiImageGenerator = aiImageGenerator;
//        this.contextQuestionGenerator = contextQuestionGenerator;
//    }
//
//    @Async
//    public CompletableFuture<GenerateImageResponseDto> asyncGenerateImageFrom(GenerateImageInputDto generateImageInputDto) throws AiException {
//        return CompletableFuture.completedFuture(aiImageGenerator.getResponseFrom(generateImageInputDto));
//    }
//
//    @Async
//    public CompletableFuture<GenerateContextQuestionResponseDto> asyncGenerateContextAndQuestionFrom(GenerateContextQuestionInputDto contextQuestionInputDto) throws AiException {
//        return CompletableFuture.completedFuture(contextQuestionGenerator.getResponseFrom(contextQuestionInputDto));
//    }
//}
