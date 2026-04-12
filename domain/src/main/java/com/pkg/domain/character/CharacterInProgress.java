package com.pkg.domain.character;

public record CharacterInProgress(
        String id,
        Long userId,
        String name,
        String appearanceKeywords,
        String personality,
        String description,
        Status status
) {

    public enum Status {
        IN_PROGRESS,
        COMPLETED
    }

    public static CharacterInProgress create(
            String id,
            Long userId,
            String name,
            String appearanceKeywords,
            String personality,
            String description
    ) {
        return new CharacterInProgress(id, userId, name, appearanceKeywords, personality, description, Status.IN_PROGRESS);
    }

    public BookCharacterCreateCommand toCreateCommand(String imageUrl) {
        if (status != Status.IN_PROGRESS) {
            throw CharacterInProgressException.alreadyCompleted(id);
        }
        return new BookCharacterCreateCommand(name, personality, description, appearanceKeywords, imageUrl, userId);
    }

    public CharacterInProgress markAsCompleted() {
        return new CharacterInProgress(id, userId, name, appearanceKeywords, personality, description, Status.COMPLETED);
    }
}
