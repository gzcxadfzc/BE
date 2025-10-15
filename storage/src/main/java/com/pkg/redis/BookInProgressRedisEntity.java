package com.pkg.redis;

public record BookInProgressRedisEntity(
        String id,
        String memberId,
        String backgroundInfo,
        BookCharacter character,
        int storyLength
) {
    record BookCharacter(
        Long id,
        String name,
        String userDescription,
        String appearanceKeywords,
        String personality
    ) {
    }
}
