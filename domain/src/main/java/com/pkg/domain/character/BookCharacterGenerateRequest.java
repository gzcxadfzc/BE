package com.pkg.domain.character;

import com.pkg.domain.member.Actor;

public record BookCharacterGenerateRequest(
        Actor creator,
        String name,
        String appearanceKeywords,
        String personality,
        String description
) {

    public BookCharacterCreateCommand toCommand(String imageUrl) {
        return new BookCharacterCreateCommand(
                name,
                personality,
                description,
                appearanceKeywords,
                imageUrl,
                creator.id()
        );
    }
}
