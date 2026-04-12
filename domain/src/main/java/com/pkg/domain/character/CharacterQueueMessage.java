package com.pkg.domain.character;

public record CharacterQueueMessage(
        String type,
        String cipId,
        String name,
        String appearanceKeywords,
        String personality,
        String description
) {
    public static CharacterQueueMessage of(String cipId, BookCharacterGenerateRequest request) {
        return new CharacterQueueMessage(
                "CHARACTER",
                cipId,
                request.name(),
                request.appearanceKeywords(),
                request.personality(),
                request.description()
        );
    }
}
