package com.pkg.domain.book;

import java.time.LocalDateTime;

public record BookThumbnail(
        String bookId,
        String title,
        String author,
        String coverImageUrl,
        LocalDateTime createdAt
) {

    public BookThumbnail(String bookId, String title, String author, String coverImageUrl, LocalDateTime createdAt) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverImageUrl = coverImageUrl;
        this.createdAt = createdAt;
    }
}
