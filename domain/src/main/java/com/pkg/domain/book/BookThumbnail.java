package com.pkg.domain.book;

import java.util.Date;

public record BookThumbnail(
        String bookId,
        String title,
        String author,
        String coverImageUrl,
        Date createdAt
) {
}
