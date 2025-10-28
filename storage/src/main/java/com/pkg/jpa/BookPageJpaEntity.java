package com.pkg.jpa;

import com.pkg.domain.book.BookPage;
import jakarta.persistence.*;

@Entity
@Table(name = "book_page")
public class BookPageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "book_id", nullable = false)
    private String bookId;
    @Column(name = "context", length = 511, nullable = false)
    private String context;
    @Column(name = "image_url", nullable = false)
    private String image_url;
    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    public static BookPageJpaEntity fromBookPage(String bookId, BookPage bookPage) {
        return BookPageJpaEntity.builder()
                .bookId(bookId)
                .context(bookPage.context())
                .imageUrl(bookPage.imageUrl())
                .pageNumber(bookPage.pageNumber())
                .build();
    }

    public BookPage toBookPage() {
        return new BookPage(
                this.context,
                this.image_url,
                this.pageNumber
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String bookId;
        private String context;
        private String imageUrl;
        private int pageNumber;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder bookId(String bookId) {
            this.bookId = bookId;
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder pageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }

        public BookPageJpaEntity build() {
            BookPageJpaEntity page = new BookPageJpaEntity();
            page.id = this.id;
            page.bookId = this.bookId;
            page.context = this.context;
            page.image_url = this.imageUrl;
            page.pageNumber = this.pageNumber;
            return page;
        }
    }
}
