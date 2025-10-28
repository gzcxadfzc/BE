package com.pkg.jpa;

import com.pkg.domain.book.Book;
import com.pkg.domain.book.BookThumbnail;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "book")
public class BookJpaEntity {

    @Id
    private String id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "character_id", nullable = false)
    private Long characterId;
    @Column(name = "title", nullable = false)
    private String title;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "book_color")
    private Long bookColor;
    @Column(name = "author", nullable = false)
    private String author;
    @Column(name = "story_length", nullable = false)
    private int storyLength;
    @Column(name = "cover_image_url")
    private String coverImageUrl;

    public String getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCharacterId() {
        return characterId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getBookColor() {
        return bookColor;
    }

    public String getAuthor() {
        return author;
    }

    public int getStoryLength() {
        return storyLength;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public static BookJpaEntity fromBook(Book book) {
        return BookJpaEntity.builder()
                .author(book.author())
                .title(book.title())
                .id(book.id())
                .coverImageUrl(book.bookPages().getFirst().imageUrl())
                .characterId(book.character().id())
                .createdAt(LocalDateTime.now())
                .userId(book.memberId())
                .storyLength(book.bookPages().size())
                .bookColor(1L)
                .build();
    }

    public BookThumbnail toBookThumbnail() {
        return new BookThumbnail(
                this.id,
                this.title,
                this.author,
                this.coverImageUrl,
                this.createdAt
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private Long userId;
        private Long characterId;
        private String title;
        private LocalDateTime createdAt;
        private Long bookColor;
        private String author;
        private int storyLength;
        private String coverImageUrl;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder characterId(Long characterId) {
            this.characterId = characterId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder bookColor(Long bookColor) {
            this.bookColor = bookColor;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder storyLength(int storyLength) {
            this.storyLength = storyLength;
            return this;
        }

        public Builder coverImageUrl(String coverImageUrl) {
            this.coverImageUrl = coverImageUrl;
            return this;
        }

        public BookJpaEntity build() {
            BookJpaEntity book = new BookJpaEntity();
            book.id = this.id;
            book.userId = this.userId;
            book.characterId = this.characterId;
            book.title = this.title;
            book.createdAt = this.createdAt;
            book.bookColor = this.bookColor;
            book.author = this.author;
            book.storyLength = this.storyLength;
            book.coverImageUrl = this.coverImageUrl;
            return book;
        }
    }
}
