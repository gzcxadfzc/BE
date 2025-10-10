package com.pkg.domain.character;

public record MainCharacter(
        String id,
        String memberId,
        String name,
        String appearanceKeywords,
        String personality,
        String imageUrl
) {
}
