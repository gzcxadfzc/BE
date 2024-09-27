package com.pkg.littlewriter.domain.bookProgressHelper;

import com.pkg.littlewriter.domain.bookProgressHelper.contextAndQuestionGenerator.ContextAndQuestionGenerator;
import com.pkg.littlewriter.domain.bookProgressHelper.exceptions.BookProgressException;
import com.pkg.littlewriter.domain.bookProgressHelper.imageGenerator.ImageGenerator;
import com.pkg.littlewriter.domain.bookProgressHelper.model.BookToProgress;
import com.pkg.littlewriter.domain.bookProgressHelper.model.ContextAndQuestion;
import com.pkg.littlewriter.domain.bookProgressHelper.model.Illustration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class AsyncBookPageHelper {
    private final ContextAndQuestionGenerator contextAndQuestionGenerator;
    private final ImageGenerator imageGenerator;

    @Autowired
    public AsyncBookPageHelper(ContextAndQuestionGenerator contextAndQuestionGenerator, ImageGenerator imageGenerator) {
        this.contextAndQuestionGenerator = contextAndQuestionGenerator;
        this.imageGenerator = imageGenerator;
    }

    @Async
    public CompletableFuture<ContextAndQuestion> asyncGenerateContextAndQuestionFrom(BookToProgress bookProgressDetails) throws BookProgressException {
        return CompletableFuture.completedFuture(contextAndQuestionGenerator.generateFrom(bookProgressDetails));
    }

    @Async
    public CompletableFuture<Illustration> asyncGenerateIllustrationFrom(BookToProgress bookProgressDetails) throws BookProgressException {
        return CompletableFuture.completedFuture(imageGenerator.generateFrom(bookProgressDetails));
    }
}
