package com.pkg.controller.book;

import com.pkg.domain.book.BookThumbnail;

import java.time.LocalDateTime;
import java.util.List;

public record BookThumbnailResponse(
        List<Thumbnail> thumbnails
) {

    record Thumbnail(
       String bookId,
       String imageUrl,
       String title,
       String author,
       LocalDateTime createdAt
    ) {
    }

    public static BookThumbnailResponse from(List<BookThumbnail> thumbnails) {
        List<Thumbnail> thumbnailList = thumbnails.stream()
                .map(bookThumbnail -> new Thumbnail(
                        bookThumbnail.bookId(),
                        bookThumbnail.coverImageUrl(),
                        bookThumbnail.title(),
                        bookThumbnail.author(),
                        bookThumbnail.createdAt()
                ))
                .toList();
        return new BookThumbnailResponse(thumbnailList);
    }
}
