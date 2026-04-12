package com.pkg.redis;

import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.character.BookCharacter;

public record BookInProgressRedisEntity(
        String id,
        Long memberId,
        String backgroundInfo,
        BookCharacterRedis character,
        int storyLength,
        Status status
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

    public enum Status {

        IN_PROGRESS,
        PENDING,
        COMPLETED,
        ;

        public static Status fromDomain(BookInProgress.Status status) {
            return switch (status) {
                case COMPLETED -> COMPLETED;
                case PENDING   -> PENDING;
                default        -> IN_PROGRESS;
            };
        }

        public static BookInProgress.Status toDomain(Status status) {
            return switch (status) {
                case COMPLETED -> BookInProgress.Status.COMPLETED;
                case PENDING   -> BookInProgress.Status.PENDING;
                default        -> BookInProgress.Status.IN_PROGRESS;
            };
        }
    }
}
