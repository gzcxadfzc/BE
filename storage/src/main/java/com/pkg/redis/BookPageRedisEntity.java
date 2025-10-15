package com.pkg.redis;

public record BookPageRedisEntity(
        String bookInProgressId,
        String Context,
        String imageUrl,
        int pageNumber
) {
}
