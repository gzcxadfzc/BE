package com.pkg.controller.bookprogress;

import com.pkg.domain.book.BookPage;
import com.pkg.domain.bookprogress.AiGenerateResult;
import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.character.BookCharacter;

import java.util.List;

public record BookInProgressResponse(
        String id,
        List<GeneratedPage> generatedPages,
        MainCharacter mainCharacter
) {
    public record GeneratedPage(
            String context,
            String imageUrl,
            int index
    ) {

        public static GeneratedPage from(BookPage bookPage) {
            return new GeneratedPage(bookPage.context(), bookPage.imageUrl(), bookPage.pageNumber());
        }
    }

    public record MainCharacter(
            Long id,
            String name,
            String personality,
            String description
    ) {

        public static MainCharacter from(BookCharacter bookCharacter) {
            return new MainCharacter(bookCharacter.id(), bookCharacter.name(), bookCharacter.personality(), bookCharacter.description());
        }
    }

    public static BookInProgressResponse from(AiGenerateResult result) {
        return new BookInProgressResponse(
                result.bookInProgress().id(),
                result.bookInProgress().previousPages()
                        .stream()
                        .map(GeneratedPage::from)
                        .toList(),
                MainCharacter.from(result.bookInProgress().character())
        );
    }

    public static BookInProgressResponse from(BookInProgress bip) {
        return new BookInProgressResponse(
                bip.id(),
                bip.previousPages()
                        .stream()
                        .map(GeneratedPage::from)
                        .toList(),
                MainCharacter.from(bip.character())
        );
    }
}
