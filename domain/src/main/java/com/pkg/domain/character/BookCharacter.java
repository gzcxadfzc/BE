package com.pkg.domain.character;


import java.util.function.Function;

public record BookCharacter(
        Long id,
        Long userId,
        String name,
        String appearanceKeywords,
        String personality,
        String description,
        String imageUrl
) {

    public static BookCharacter fromCommand(BookCharacterCreateCommand command, Function<BookCharacterCreateCommand, BookCharacter> bookCharacterFactoryMethod) {
        return bookCharacterFactoryMethod.apply(command);
    }
}
