package com.pkg.adapter;

import com.pkg.domain.ai.BookPageGenerator;
import com.pkg.domain.ai.BookPageGenerated;
import com.pkg.domain.bookprogress.BookToProgress;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class BookPageGeneratorAdapter implements BookPageGenerator {

    private final OpenAiImageGenerateChain imageGenerateChain;
    private final OpenAiContextQuestionChain contextQuestionChain;

    public BookPageGeneratorAdapter(OpenAiImageGenerateChain imageGenerateChain, OpenAiContextQuestionChain contextQuestionGenerator) {
        this.imageGenerateChain = imageGenerateChain;
        this.contextQuestionChain = contextQuestionGenerator;
    }

    @Override
    public BookPageGenerated generatePageFrom(BookToProgress bookToProgress) {
        BookProgressInfo bookProgressInfo = BookProgressInfo.fromBookToProgress(bookToProgress);
        CompletableFuture<String> imageUrlFuture =  imageGenerateChain.asynGetImageurl(bookProgressInfo);
        CompletableFuture<ContextQuestion> contextQuestionFuture = contextQuestionChain.asyncGetContextQuestion(bookProgressInfo);

        return imageUrlFuture
                .thenCombine(contextQuestionFuture, (imageUrl, contextQuestion) ->
                        new BookPageGenerated(
                                imageUrl,
                                contextQuestion.context(),
                                contextQuestion.questions()
                        )
                ).join();
    }
}
