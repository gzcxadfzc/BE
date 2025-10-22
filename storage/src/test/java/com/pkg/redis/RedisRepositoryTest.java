package com.pkg.redis;

import com.pkg.config.RedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataRedisTest
@Import({
        RedisConfig.class,
        BookInProgressRedisRepository.class,
        BookPageRedisRepository.class
})
class RedisRepositoryTest {

    @Autowired
    private BookInProgressRedisRepository bookInProgressRedisRepository;

    @Autowired
    private BookPageRedisRepository bookPageRedisRepository;

    @AfterEach
    void cleanup() {
        // Clean up test data after each test
        bookPageRedisRepository.deleteAll("test-book-1");
        bookPageRedisRepository.deleteAll("test-book-2");

        // Clean up BookInProgress test data
        String[] bookIds = {
                "test-book-1", "test-book-2", "book-id-1", "book-id-2",
                "book-id-3", "test-book-id-1", "test-book-id-2"
        };

        for (String bookId : bookIds) {
            if (bookInProgressRedisRepository.has(bookId)) {
                bookInProgressRedisRepository.delete(bookId);
            }
        }
    }

    // ==================== BookInProgressRedisRepository Tests ====================

    @Test
    @DisplayName("BookInProgress: put과 get으로 저장 및 조회 확인")
    void testBookInProgressPutAndGet() {
        // Given
        Long userId = 1L;
        BookInProgressRedisEntity.BookCharacterRedis character = new BookInProgressRedisEntity.BookCharacterRedis(
                100L,
                "테스트 캐릭터",
                "용감한 소년",
                "검은 머리, 파란 눈",
                "활발하고 긍정적인 성격",
                "example.com"
        );

        BookInProgressRedisEntity entity = new BookInProgressRedisEntity(
                "book-id-1",
                userId,
                "마법의 숲에서의 모험",
                character,
                10
        );

        // When
        bookInProgressRedisRepository.put(entity);
        BookInProgressRedisEntity retrieved = bookInProgressRedisRepository.get("book-id-1");

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo("book-id-1");
        assertThat(retrieved.memberId()).isEqualTo(userId);
        assertThat(retrieved.backgroundInfo()).isEqualTo("마법의 숲에서의 모험");
        assertThat(retrieved.storyLength()).isEqualTo(10);
        assertThat(retrieved.character()).isNotNull();
        assertThat(retrieved.character().id()).isEqualTo(100L);
        assertThat(retrieved.character().name()).isEqualTo("테스트 캐릭터");
        assertThat(retrieved.character().userDescription()).isEqualTo("용감한 소년");
        assertThat(retrieved.character().appearanceKeywords()).isEqualTo("검은 머리, 파란 눈");
        assertThat(retrieved.character().personality()).isEqualTo("활발하고 긍정적인 성격");

        System.out.println("BookInProgress put and get operations successful");
    }

    @Test
    @DisplayName("BookInProgress: existsByUserId로 존재 여부 확인")
    void testBookInProgressExists() {
        // Given
        Long userId = 1L;
        String testBookId = "test-book-id-1";
        BookInProgressRedisEntity.BookCharacterRedis character = new BookInProgressRedisEntity.BookCharacterRedis(
                100L, "캐릭터", "설명", "외모", "성격", "example.com"
        );
        BookInProgressRedisEntity entity = new BookInProgressRedisEntity(
                testBookId, userId, "배경", character, 5
        );

        // When - 저장 전
        boolean existsBefore = bookInProgressRedisRepository.has(testBookId);

        // Then - 저장 전에는 존재하지 않음
        assertThat(existsBefore).isFalse();

        // When - 저장 후
        bookInProgressRedisRepository.put(entity);
        boolean existsAfter = bookInProgressRedisRepository.has(testBookId);

        // Then - 저장 후에는 존재함
        assertThat(existsAfter).isTrue();

        System.out.println("BookInProgress existsByUserId operation successful");
    }

    @Test
    @DisplayName("BookInProgress: deleteByUserId로 삭제 확인")
    void testBookInProgressDelete() {
        // Given
        Long userId = 1L;
        String testBookId = "test-book-id-1";
        BookInProgressRedisEntity.BookCharacterRedis character = new BookInProgressRedisEntity.BookCharacterRedis(
                100L, "캐릭터", "설명", "외모", "성격", "example.com"
        );
        BookInProgressRedisEntity entity = new BookInProgressRedisEntity(
                testBookId, userId, "배경", character, 5
        );
        bookInProgressRedisRepository.put(entity);

        // When
        bookInProgressRedisRepository.delete(testBookId);
        boolean existsAfterDelete = bookInProgressRedisRepository.has(testBookId);

        // Then
        assertThat(existsAfterDelete).isFalse();

        System.out.println("BookInProgress deleteByUserId operation successful");
    }

    @Test
    @DisplayName("BookInProgress: 여러 사용자의 데이터를 독립적으로 저장 및 조회")
    void testBookInProgressMultipleUsers() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        String testBookId1 = "test-book-id-1";
        String testBookId2 = "test-book-id-2";


        BookInProgressRedisEntity.BookCharacterRedis character1 = new BookInProgressRedisEntity.BookCharacterRedis(
                100L, "캐릭터1", "설명1", "외모1", "성격1", "example.com"
        );
        BookInProgressRedisEntity entity1 = new BookInProgressRedisEntity(
                testBookId1, userId1, "배경1", character1, 5
        );

        BookInProgressRedisEntity.BookCharacterRedis character2 = new BookInProgressRedisEntity.BookCharacterRedis(
                200L, "캐릭터2", "설명2", "외모2", "성격2", "example2.com"
        );
        BookInProgressRedisEntity entity2 = new BookInProgressRedisEntity(
                testBookId2, userId2, "배경2", character2, 10
        );

        // When
        bookInProgressRedisRepository.put(entity1);
        bookInProgressRedisRepository.put(entity2);

        BookInProgressRedisEntity retrieved1 = bookInProgressRedisRepository.get(testBookId1);
        BookInProgressRedisEntity retrieved2 = bookInProgressRedisRepository.get(testBookId2);

        // Then
        assertThat(retrieved1).isNotNull();
        assertThat(retrieved1.id()).isEqualTo(testBookId1);
        assertThat(retrieved1.character().name()).isEqualTo("캐릭터1");

        assertThat(retrieved2).isNotNull();
        assertThat(retrieved2.id()).isEqualTo(testBookId2);
        assertThat(retrieved2.character().name()).isEqualTo("캐릭터2");

        System.out.println("BookInProgress multiple users operations successful");
    }

    @Test
    @DisplayName("BookInProgress: 존재하지 않는 id로 조회 시 null 반환")
    void testBookInProgressGetNonExistent() {
        // Given
        Long nonExistentUserId = 999L;

        // When
        BookInProgressRedisEntity retrieved = bookInProgressRedisRepository.get("non-exist-id");

        // Then
        assertThat(retrieved).isNull();

        System.out.println("BookInProgress get non-existent operation successful");
    }

    @Test
    @DisplayName("BookInProgress: findByMemberId로 특정 회원의 모든 진행중인 책 조회")
    void testFindByMemberId() {
        // Given
        Long memberId = 123L;
        BookInProgressRedisEntity.BookCharacterRedis character1 = new BookInProgressRedisEntity.BookCharacterRedis(
                100L, "캐릭터1", "설명1", "외모1", "성격1", "example.com"
        );
        BookInProgressRedisEntity entity1 = new BookInProgressRedisEntity(
                "book-id-1", memberId, "배경1", character1, 5
        );

        BookInProgressRedisEntity.BookCharacterRedis character2 = new BookInProgressRedisEntity.BookCharacterRedis(
                200L, "캐릭터2", "설명2", "외모2", "성격2", "example.com"
        );
        BookInProgressRedisEntity entity2 = new BookInProgressRedisEntity(
                "book-id-2", memberId, "배경2", character2, 10
        );

        // When
        bookInProgressRedisRepository.put(entity1);
        bookInProgressRedisRepository.put(entity2);

        List<BookInProgressRedisEntity> foundBooks = bookInProgressRedisRepository.findByMemberId(memberId);

        // Then
        assertThat(foundBooks).isNotNull();
        assertThat(foundBooks).hasSize(2);
        assertThat(foundBooks).extracting(BookInProgressRedisEntity::id)
                .containsExactlyInAnyOrder("book-id-1", "book-id-2");
        assertThat(foundBooks).extracting(BookInProgressRedisEntity::memberId)
                .containsOnly(memberId);

        System.out.println("BookInProgress findByMemberId operation successful");
    }

    @Test
    @DisplayName("BookInProgress: findByMemberId로 존재하지 않는 회원 조회 시 빈 리스트 반환")
    void testFindByMemberIdNonExistent() {
        // Given
        Long nonExistentMemberId = -1L;

        // When
        List<BookInProgressRedisEntity> foundBooks = bookInProgressRedisRepository.findByMemberId(nonExistentMemberId);

        // Then
        assertThat(foundBooks).isNotNull();
        assertThat(foundBooks).isEmpty();

        System.out.println("BookInProgress findByMemberId with non-existent member successful");
    }

    @Test
    @DisplayName("BookInProgress: findByMemberId로 여러 회원의 책을 독립적으로 조회")
    void testFindByMemberIdMultipleMembers() {
        // Given
        Long memberId1 = 111L;
        Long memberId2 = 222L;

        BookInProgressRedisEntity.BookCharacterRedis character1 = new BookInProgressRedisEntity.BookCharacterRedis(
                100L, "캐릭터1", "설명1", "외모1", "성격1", "example.com"
        );
        BookInProgressRedisEntity entity1 = new BookInProgressRedisEntity(
                "book-id-1", memberId1, "배경1", character1, 5
        );

        BookInProgressRedisEntity.BookCharacterRedis character2 = new BookInProgressRedisEntity.BookCharacterRedis(
                200L, "캐릭터2", "설명2", "외모2", "성격2", "example.com"
        );
        BookInProgressRedisEntity entity2 = new BookInProgressRedisEntity(
                "book-id-2", memberId1, "배경2", character2, 10
        );

        BookInProgressRedisEntity.BookCharacterRedis character3 = new BookInProgressRedisEntity.BookCharacterRedis(
                300L, "캐릭터3", "설명3", "외모3", "성격3", "example.com"
        );
        BookInProgressRedisEntity entity3 = new BookInProgressRedisEntity(
                "book-id-3", memberId2, "배경3", character3, 15
        );

        // When
        bookInProgressRedisRepository.put(entity1);
        bookInProgressRedisRepository.put(entity2);
        bookInProgressRedisRepository.put(entity3);

        List<BookInProgressRedisEntity> member1Books = bookInProgressRedisRepository.findByMemberId(memberId1);
        List<BookInProgressRedisEntity> member2Books = bookInProgressRedisRepository.findByMemberId(memberId2);

        // Then
        assertThat(member1Books).hasSize(2);
        assertThat(member1Books).extracting(BookInProgressRedisEntity::id)
                .containsExactlyInAnyOrder("book-id-1", "book-id-2");
        assertThat(member1Books).extracting(BookInProgressRedisEntity::memberId)
                .containsOnly(memberId1);

        assertThat(member2Books).hasSize(1);
        assertThat(member2Books).extracting(BookInProgressRedisEntity::id)
                .containsExactly("book-id-3");
        assertThat(member2Books).extracting(BookInProgressRedisEntity::memberId)
                .containsOnly(memberId2);

        System.out.println("BookInProgress findByMemberId with multiple members successful");
    }

    @Test
    @DisplayName("BookInProgress: 책 삭제 후 findByMemberId 결과에 반영되는지 확인")
    void testFindByMemberIdAfterDelete() {
        // Given
        Long memberId = 123L;
        BookInProgressRedisEntity.BookCharacterRedis character1 = new BookInProgressRedisEntity.BookCharacterRedis(
                100L, "캐릭터1", "설명1", "외모1", "성격1", "example.com"
        );
        BookInProgressRedisEntity entity1 = new BookInProgressRedisEntity(
                "book-id-1", memberId, "배경1", character1, 5
        );

        BookInProgressRedisEntity.BookCharacterRedis character2 = new BookInProgressRedisEntity.BookCharacterRedis(
                200L, "캐릭터2", "설명2", "외모2", "성격2", "example.com"
        );
        BookInProgressRedisEntity entity2 = new BookInProgressRedisEntity(
                "book-id-2", memberId, "배경2", character2, 10
        );

        bookInProgressRedisRepository.put(entity1);
        bookInProgressRedisRepository.put(entity2);

        // When - 첫 번째 책 삭제
        bookInProgressRedisRepository.delete("book-id-1");
        List<BookInProgressRedisEntity> foundBooksAfterDelete = bookInProgressRedisRepository.findByMemberId(memberId);

        // Then - 삭제된 책은 조회되지 않아야 함 (단, Set에서는 제거되지 않으므로 null이 포함될 수 있음)
        assertThat(foundBooksAfterDelete).isNotNull();
        // null 값을 필터링한 결과 확인
        List<BookInProgressRedisEntity> nonNullBooks = foundBooksAfterDelete.stream()
                .filter(book -> book != null)
                .toList();
        assertThat(nonNullBooks).hasSize(1);
        assertThat(nonNullBooks.get(0).id()).isEqualTo("book-id-2");

        System.out.println("BookInProgress findByMemberId after delete operation successful");
    }

    // ==================== BookPageRedisRepository Tests ====================

    @Test
    @DisplayName("BookPage: append로 페이지 추가 및 retrieveAll로 조회")
    void testBookPageAppendAndRetrieveAll() {
        // Given
        String bookId = "test-book-1";
        BookPageRedisEntity page1 = new BookPageRedisEntity(
                bookId, "첫 번째 페이지 내용", "http://example.com/image1.jpg", 1
        );
        BookPageRedisEntity page2 = new BookPageRedisEntity(
                bookId, "두 번째 페이지 내용", "http://example.com/image2.jpg", 2
        );
        BookPageRedisEntity page3 = new BookPageRedisEntity(
                bookId, "세 번째 페이지 내용", "http://example.com/image3.jpg", 3
        );

        // When
        bookPageRedisRepository.append(bookId, page1);
        bookPageRedisRepository.append(bookId, page2);
        bookPageRedisRepository.append(bookId, page3);

        List<BookPageRedisEntity> retrievedPages = bookPageRedisRepository.retrieveAll(bookId);

        // Then
        assertThat(retrievedPages).isNotNull();
        assertThat(retrievedPages).hasSize(3);

        assertThat(retrievedPages.get(0).context()).isEqualTo("첫 번째 페이지 내용");
        assertThat(retrievedPages.get(0).pageNumber()).isEqualTo(1);
        assertThat(retrievedPages.get(0).imageUrl()).isEqualTo("http://example.com/image1.jpg");

        assertThat(retrievedPages.get(1).context()).isEqualTo("두 번째 페이지 내용");
        assertThat(retrievedPages.get(1).pageNumber()).isEqualTo(2);

        assertThat(retrievedPages.get(2).context()).isEqualTo("세 번째 페이지 내용");
        assertThat(retrievedPages.get(2).pageNumber()).isEqualTo(3);

        System.out.println("BookPage append and retrieveAll operations successful");
    }

    @Test
    @DisplayName("BookPage: 빈 bookId로 조회 시 빈 리스트 반환")
    void testBookPageRetrieveAllEmpty() {
        // Given
        String emptyBookId = "non-existent-book";

        // When
        List<BookPageRedisEntity> retrievedPages = bookPageRedisRepository.retrieveAll(emptyBookId);

        // Then
        assertThat(retrievedPages).isNotNull();
        assertThat(retrievedPages).isEmpty();

        System.out.println("BookPage retrieveAll empty operation successful");
    }

    @Test
    @DisplayName("BookPage: deleteAll로 모든 페이지 삭제")
    void testBookPageDeleteAll() {
        // Given
        String bookId = "test-book-1";
        BookPageRedisEntity page1 = new BookPageRedisEntity(
                bookId, "페이지 1", "http://example.com/image1.jpg", 1
        );
        BookPageRedisEntity page2 = new BookPageRedisEntity(
                bookId, "페이지 2", "http://example.com/image2.jpg", 2
        );

        bookPageRedisRepository.append(bookId, page1);
        bookPageRedisRepository.append(bookId, page2);

        // When
        List<BookPageRedisEntity> beforeDelete = bookPageRedisRepository.retrieveAll(bookId);
        bookPageRedisRepository.deleteAll(bookId);
        List<BookPageRedisEntity> afterDelete = bookPageRedisRepository.retrieveAll(bookId);

        // Then
        assertThat(beforeDelete).hasSize(2);
        assertThat(afterDelete).isEmpty();

        System.out.println("BookPage deleteAll operation successful");
    }

    @Test
    @DisplayName("BookPage: 여러 book의 페이지를 독립적으로 관리")
    void testBookPageMultipleBooks() {
        // Given
        String bookId1 = "test-book-1";
        String bookId2 = "test-book-2";

        BookPageRedisEntity book1Page1 = new BookPageRedisEntity(
                bookId1, "책1 페이지1", "http://example.com/book1/page1.jpg", 1
        );
        BookPageRedisEntity book1Page2 = new BookPageRedisEntity(
                bookId1, "책1 페이지2", "http://example.com/book1/page2.jpg", 2
        );

        BookPageRedisEntity book2Page1 = new BookPageRedisEntity(
                bookId2, "책2 페이지1", "http://example.com/book2/page1.jpg", 1
        );
        BookPageRedisEntity book2Page2 = new BookPageRedisEntity(
                bookId2, "책2 페이지2", "http://example.com/book2/page2.jpg", 2
        );
        BookPageRedisEntity book2Page3 = new BookPageRedisEntity(
                bookId2, "책2 페이지3", "http://example.com/book2/page3.jpg", 3
        );

        // When
        bookPageRedisRepository.append(bookId1, book1Page1);
        bookPageRedisRepository.append(bookId1, book1Page2);

        bookPageRedisRepository.append(bookId2, book2Page1);
        bookPageRedisRepository.append(bookId2, book2Page2);
        bookPageRedisRepository.append(bookId2, book2Page3);

        List<BookPageRedisEntity> book1Pages = bookPageRedisRepository.retrieveAll(bookId1);
        List<BookPageRedisEntity> book2Pages = bookPageRedisRepository.retrieveAll(bookId2);

        // Then
        assertThat(book1Pages).hasSize(2);
        assertThat(book1Pages.get(0).context()).isEqualTo("책1 페이지1");
        assertThat(book1Pages.get(1).context()).isEqualTo("책1 페이지2");

        assertThat(book2Pages).hasSize(3);
        assertThat(book2Pages.get(0).context()).isEqualTo("책2 페이지1");
        assertThat(book2Pages.get(1).context()).isEqualTo("책2 페이지2");
        assertThat(book2Pages.get(2).context()).isEqualTo("책2 페이지3");

        System.out.println("BookPage multiple books operations successful");
    }

    @Test
    @DisplayName("BookPage: append 순서가 retrieveAll 순서와 일치하는지 확인")
    void testBookPageOrder() {
        // Given
        String bookId = "test-book-1";

        // When - 순서대로 추가
        for (int i = 1; i <= 5; i++) {
            BookPageRedisEntity page = new BookPageRedisEntity(
                    bookId, "페이지 " + i, "http://example.com/image" + i + ".jpg", i
            );
            bookPageRedisRepository.append(bookId, page);
        }

        List<BookPageRedisEntity> retrievedPages = bookPageRedisRepository.retrieveAll(bookId);

        // Then - 순서가 유지되는지 확인
        assertThat(retrievedPages).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(retrievedPages.get(i).pageNumber()).isEqualTo(i + 1);
            assertThat(retrievedPages.get(i).context()).isEqualTo("페이지 " + (i + 1));
        }

        System.out.println("BookPage order preservation operation successful");
    }

    @Test
    @DisplayName("BookPage: 특정 book만 삭제하고 다른 book은 유지")
    void testBookPageDeleteSpecificBook() {
        // Given
        String bookId1 = "test-book-1";
        String bookId2 = "test-book-2";

        bookPageRedisRepository.append(bookId1, new BookPageRedisEntity(bookId1, "책1 내용", "url1", 1));
        bookPageRedisRepository.append(bookId2, new BookPageRedisEntity(bookId2, "책2 내용", "url2", 1));

        // When
        bookPageRedisRepository.deleteAll(bookId1);

        List<BookPageRedisEntity> book1Pages = bookPageRedisRepository.retrieveAll(bookId1);
        List<BookPageRedisEntity> book2Pages = bookPageRedisRepository.retrieveAll(bookId2);

        // Then
        assertThat(book1Pages).isEmpty();
        assertThat(book2Pages).hasSize(1);
        assertThat(book2Pages.get(0).context()).isEqualTo("책2 내용");

        System.out.println("BookPage delete specific book operation successful");
    }
}
