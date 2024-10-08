package com.pkg.littlewriter.domain.bookProgressHelper;

import com.pkg.littlewriter.domain.bookProgressHelper.exceptions.BookProgressException;
import com.pkg.littlewriter.domain.bookProgressHelper.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class BookPageProgressService implements BookPageProgressHelper {
    private final AsyncBookPageHelper asyncBookPageHelper;

    @Autowired
    BookPageProgressService(AsyncBookPageHelper asyncBookPageHelper) {
        this.asyncBookPageHelper = asyncBookPageHelper;
    }

    @Override
    public BookPage continueStory(BookToProgress bookToProgress) throws BookProgressException {
        try {
            CompletableFuture<Illustration> illustrationCompletableFuture = asyncBookPageHelper.asyncGenerateIllustrationFrom(bookToProgress);
            CompletableFuture<ContextAndQuestion> contextAndQuestionCompletableFuture = asyncBookPageHelper.asyncGenerateContextAndQuestionFrom(bookToProgress);
            return illustrationCompletableFuture.thenCombine(contextAndQuestionCompletableFuture, this::createBookPage).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BookProgressException(e.getMessage());
        }
    }

    private BookPage createBookPage(Illustration illustration, ContextAndQuestion contextAndQuestion) {
        return BookPage.builder()
                .illustration(illustration)
                .contextAndQuestion(contextAndQuestion)
                .build();
    }
}
