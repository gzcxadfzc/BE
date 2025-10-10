package com.pkg.domain.book;

public record BookFinishRequest(
        String bookInProgressId,
        String title,
        String memberId,
        String author
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String bookInProgressId;
        private String title;
        private String memberId;
        private String author;

        public Builder bookInProgressId(String bookInProgressId) {
            this.bookInProgressId = bookInProgressId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder memberId(String memberId) {
            this.memberId = memberId;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public BookFinishRequest build() {
            return new BookFinishRequest(
                    bookInProgressId,
                    title,
                    memberId,
                    author
            );
        }
    }
}
