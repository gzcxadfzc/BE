package com.pkg.adapter;

import com.pkg.core.CallStep;
import com.pkg.core.CallStepChain;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class OpenAiImageGenerateChain {

    private final CallStep<BookProgressInfo, String> imageCreationChain;

    public OpenAiImageGenerateChain(OpenAiBookImageGenerator imageGenerator, OpenAiContextDepictGenerator depictGenerator) {
        this.imageCreationChain = CallStepChain.startWith(depictGenerator)
                .add(imageGenerator)
                .toCallStep();
    }

    @Async("open-ai-api")
    public CompletableFuture<String> asynGetImageurl(BookProgressInfo input) {
        return CompletableFuture.completedFuture(imageCreationChain.operate(input));
    }
}
