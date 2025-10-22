package com.pkg.domain.book;

import com.pkg.domain.character.BookCharacter;

import java.util.List;

public record Book(
        String id,
        Long memberId,
        List<BookPage> bookPages,
        String title,
        String author,
        BookCharacter character
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String id;
        private Long memberId;
        private List<BookPage> bookPages;
        private String title;
        private String author;
        private BookCharacter character;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder memberId(Long memberId) {
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

        public Builder character(BookCharacter character) {
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
