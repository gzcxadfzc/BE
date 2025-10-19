package com.pkg.adapter;

import com.pkg.core.LLMChain;
import com.pkg.core.LLMStep;
import com.pkg.domain.ai.BookPageGenerator;
import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.book.BookInitRequest;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.bookprogress.BookToProgress;
import org.springframework.stereotype.Component;

@Component
public class BookPageGeneratorAdapter implements BookPageGenerator {

    @Override
    public BookInProgress initBook(BookInitRequest bookInit) {
        return null;
    }

    @Override
    public BookPage generatePageFrom(BookToProgress bookToProgress) {
       LLMChain chain = LLMChain.builder()
                .add(new LLMStep((input)->input, 1))
                .add(new LLMStep((input)->input, 1))
                .build();
       chain.operate(bookToProgress.userInput());

        return null;
    }
}
