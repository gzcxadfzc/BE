package com.pkg.controller.character;

import com.pkg.domain.character.BookCharacter;

public record BookCharacterResponse(
        Long id,
        String name,
        String personality,
        String imageUrl,
        String description
) {

    public static BookCharacterResponse fromBookCharacter(BookCharacter bookCharacter) {
        return new BookCharacterResponse(
                bookCharacter.id(),
                bookCharacter.name(),
                bookCharacter.personality(),
                bookCharacter.imageUrl(),
                bookCharacter.description()
        );
    }
}
