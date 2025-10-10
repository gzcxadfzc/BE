package com.pkg.redis;

import java.util.List;

public record BookInProgressRedisEntity(
        String backgroundInfo,
        String bookId,
        Long characterId,
        Long userId,
        List<Page> previousPages,
        int storyLength
) {

    record Page(
        Long id,
        String bookId,
        String context,
        String colorImageUrl,
        String sketchImageUrl,
        String actionInfo,
        int pageNumber
    ) {
    }
}
