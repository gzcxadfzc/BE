package com.pkg.redis;

import com.pkg.domain.book.BookPage;

public record BookPageRedisEntity(
        String bookInProgressId,
        String context,
        String imageUrl,
        int pageNumber
) {

    public BookPage toBookPage() {
        return new BookPage(
            this.context,
            this.imageUrl,
            this.pageNumber
        );
    }
}
