package com.pkg.domain.character;

public record BookCharacterCreateCommand(
        String name,
        String personality,
        String description,
        String appearanceKeywords,
        String imageUrl,
        Long userId
) {
}
