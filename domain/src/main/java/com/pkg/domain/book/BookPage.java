package com.pkg.domain.book;

public record BookPage(
    String context,
    String imageUrl,
    int pageNumber
) {

    public BookPage changeImageUrl(String url) {
        return new BookPage(this.context, url, this.pageNumber);
    }
}
