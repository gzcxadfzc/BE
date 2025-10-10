package com.pkg.domain.book;

import java.util.List;

public record Book(
        String id,
        String memberId,
        List<BookPage> bookPages,
        String title,
        String author,
        Character character
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String id;
        private String memberId;
        private List<BookPage> bookPages;
        private String title;
        private String author;
        private Character character;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder memberId(String memberId) {
            this.memberId = memberId;
            return this;
        }

        public Builder bookPages(List<BookPage> bookPages) {
            this.bookPages = bookPages;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder character(Character character) {
            this.character = character;
            return this;
        }

        public Book build() {
            return new Book(
              id,
              memberId,
              bookPages,
              title,
              author,
              character
            );
        }
    }
}
