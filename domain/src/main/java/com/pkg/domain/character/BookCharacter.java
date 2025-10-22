package com.pkg.domain.character;


public record BookCharacter(
        Long id,
        Long userId,
        String name,
        String appearanceKeywords,
        String personality,
        String description,
        String imageUrl
) {
}
