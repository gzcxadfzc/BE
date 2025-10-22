package com.pkg.jpa;

public record BookCharacterProjection(
        String bookId,
        Long userId,
        String title,
        String author,
        Long characterId,
        String characterName,
        String characterAppearanceKeyword,
        String characterPersonality,
        String characterDescription,
        String characterImageUrl
) {
}