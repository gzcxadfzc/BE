package com.pkg.jpa;

import com.pkg.StorageTestApplication;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test-in-memory")
@DisplayName("BookJpaEntity JPA Mapping Tests")
@ContextConfiguration(classes = {StorageTestApplication.class})
class BookJpaEntityTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("Book 엔티티 저장 및 조회 테스트")
    void testBookEntityPersistence() {
        // Given
        BookJpaEntity book = BookJpaEntity.builder()
            .id("book-uuid-123")
            .userId(1L)
            .characterId(1L)
            .title("My Story")
            .createdAt(LocalDateTime.now())
            .bookColor(0xFF0000L)
            .author("Test Author")
            .storyLength(10)
            .coverImageUrl("https://example.com/cover.jpg")
            .build();

        // When
        entityManager.persist(book);
        entityManager.flush();
        entityManager.clear();

        // Then
        BookJpaEntity foundBook = entityManager.find(BookJpaEntity.class, "book-uuid-123");
        assertThat(foundBook).isNotNull();
        assertThat(foundBook.getId()).isEqualTo("book-uuid-123");
        assertThat(foundBook.getUserId()).isEqualTo(1L);
        assertThat(foundBook.getCharacterId()).isEqualTo(1L);
        assertThat(foundBook.getTitle()).isEqualTo("My Story");
        assertThat(foundBook.getAuthor()).isEqualTo("Test Author");
        assertThat(foundBook.getStoryLength()).isEqualTo(10);
        assertThat(foundBook.getCoverImageUrl()).isEqualTo("https://example.com/cover.jpg");
    }
}
