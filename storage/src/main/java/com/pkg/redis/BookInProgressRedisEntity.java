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
        COMPLETED,
        ;

        public static Status fromDomain(BookInProgress.Status status) {
            if(status.equals(BookInProgress.Status.COMPLETED)) {
                return COMPLETED;
            }
            return IN_PROGRESS;
        }

        public static BookInProgress.Status toDomain(Status status) {
            if(status == COMPLETED) {
                return BookInProgress.Status.COMPLETED;
            }
            return BookInProgress.Status.IN_PROGRESS;
        }
    }
}
