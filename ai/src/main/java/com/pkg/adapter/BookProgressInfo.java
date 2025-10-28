package com.pkg.adapter;

import com.pkg.domain.book.BookPage;
import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.bookprogress.BookToProgress;
import com.pkg.domain.character.BookCharacter;

import java.util.stream.Collectors;

public record BookProgressInfo (
        String previousContext,
        String currentContext,
        CharacterInfo characterInfo
){
    record CharacterInfo(
            String name,
            String personality,
            String appearance
    ) {
    }

    public static BookProgressInfo fromBookToProgress(BookToProgress bookToProgress) {
        BookInProgress bip = bookToProgress.bookInProgress();
        BookCharacter character = bookToProgress.bookInProgress().character();

        CharacterInfo characterInfo = new CharacterInfo(
                character.name(),
                character.description(),
                character.appearanceKeywords()
        );

        return new BookProgressInfo(
                bip.previousPages()
                        .stream()
                        .map(BookPage::context)
                        .collect(Collectors.joining()),
                bookToProgress.userInput(),
                characterInfo
        );
    }
}
