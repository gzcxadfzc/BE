package com.pkg.redis;

import com.pkg.domain.character.BookCharacter;

public record BookInProgressRedisEntity(
        String id,
        Long memberId,
        String backgroundInfo,
        BookCharacterRedis character,
        int storyLength
) {

    public BookCharacter toBookCharacter() {
        return new BookCharacter(
                this.character.id,
                this.memberId,
                this.character.name,
                this.character.appearanceKeywords,
                this.character.personality,
                this.character.userDescription,
                this.character.imageUrl
        );
    }

    record BookCharacterRedis (
        Long id,
        String name,
        String userDescription,
        String appearanceKeywords,
        String personality,
        String imageUrl
    ) {
    }
}
