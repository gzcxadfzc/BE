package com.pkg.redis;

import com.pkg.config.RedisConfig;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.character.BookCharacter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ActiveProfiles("test")
@DataRedisTest
@Import({
        RedisConfig.class,
        BookInProgressRedisRepository.class,
        BookPageRedisRepository.class,
        BookInProgressRepositoryAdapter.class
})
@DisplayName("BookInProgressRepositoryAdapter 통합 테스트")
class BookInProgressRepositoryAdapterTest {

    @Autowired
    private BookInProgressRepositoryAdapter adapter;

    @Autowired
    private BookInProgressRedisRepository redisRepository;

    @Autowired
    private BookPageRedisRepository pageRedisRepository;

    private BookCharacter testCharacter;
    private BookInProgress testBookInProgress;

    @BeforeEach
    void setUp() {
        testCharacter = new BookCharacter(
                100L,
                1L,
                "토끼 토리",
                "흰색 털, 긴 귀, 분홍색 코",
                "호기심 많고 용감한 성격",
                "숲속에 사는 착한 토끼",
                "http://example.com/rabbit.png"
        );

        testBookInProgress = new BookInProgress(
                "test-book-001",
                1L,
                "숲속 친구들의 모험 이야기",
                testCharacter,
                Collections.emptyList(),
                BookInProgress.Status.IN_PROGRESS
        );
    }

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;

    @BeforeEach
    void flushRedis() {
        stringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @AfterEach
    void cleanup() {
        stringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    // ==================== save() 메서드 테스트 ====================

    @Test
    @DisplayName("save: 새로운 BookInProgress를 저장할 수 있다")
    void testSaveNewBookInProgress() {
        // When
        BookInProgress saved = adapter.save(testBookInProgress);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.id()).isEqualTo("test-book-001");
        assertThat(saved.ownerId()).isEqualTo(1L);
        assertThat(saved.backgroundInfo()).isEqualTo("숲속 친구들의 모험 이야기");
        assertThat(saved.character()).isNotNull();
        assertThat(saved.character().name()).isEqualTo("토끼 토리");
        assertThat(saved.previousPages()).isEmpty();

        // Redis에 실제로 저장되었는지 확인
        BookInProgressRedisEntity redisEntity = redisRepository.get("test-book-001");
        assertThat(redisEntity).isNotNull();
        assertThat(redisEntity.id()).isEqualTo("test-book-001");

        System.out.println("save: New BookInProgress saved successfully");
    }

    @Test
    @DisplayName("save: 페이지가 있는 BookInProgress를 저장할 수 있다")
    void testSaveBookInProgressWithPages() {
        // Given
        List<BookPage> pages = Arrays.asList(
                new BookPage("첫 번째 페이지 내용", "http://example.com/page1.png", 1),
                new BookPage("두 번째 페이지 내용", "http://example.com/page2.png", 2),
                new BookPage("세 번째 페이지 내용", "http://example.com/page3.png", 3)
        );

        BookInProgress bookWithPages = new BookInProgress(
                "book-with-pages",
                1L,
                "페이지가 있는 책",
                testCharacter,
                pages,
                BookInProgress.Status.IN_PROGRESS

        );

        // When
        BookInProgress saved = adapter.save(bookWithPages);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.previousPages()).hasSize(3);
        assertThat(saved.previousPages().get(0).context()).isEqualTo("첫 번째 페이지 내용");
        assertThat(saved.previousPages().get(1).context()).isEqualTo("두 번째 페이지 내용");
        assertThat(saved.previousPages().get(2).context()).isEqualTo("세 번째 페이지 내용");

        // Redis에 페이지가 저장되었는지 확인
        List<BookPageRedisEntity> savedPages = pageRedisRepository.retrieveAll("book-with-pages");
        assertThat(savedPages).hasSize(3);
        assertThat(savedPages.get(0).context()).isEqualTo("첫 번째 페이지 내용");
        assertThat(savedPages.get(0).pageNumber()).isEqualTo(1);

        System.out.println("save: BookInProgress with pages saved successfully");
    }

    @Test
    @DisplayName("save: 기존 BookInProgress를 업데이트할 수 있다")
    void testUpdateExistingBookInProgress() {
        // Given - 먼저 저장
        adapter.save(testBookInProgress);

        // When - 페이지를 추가하고 다시 저장
        BookPage newPage = new BookPage("새로운 페이지", "http://example.com/new.png", 1);
        BookInProgress updated = testBookInProgress.addBookPage(newPage);
        BookInProgress saved = adapter.save(updated);

        // Then
        assertThat(saved.previousPages()).hasSize(1);
        assertThat(saved.previousPages().get(0).context()).isEqualTo("새로운 페이지");

        // Redis에서 조회하여 확인
        BookInProgress retrieved = adapter.retrieveById("test-book-001");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.previousPages()).hasSize(1);
        assertThat(retrieved.previousPages().get(0).context()).isEqualTo("새로운 페이지");

        System.out.println("save: Existing BookInProgress updated successfully");
    }

    @Test
    @DisplayName("save: 페이지를 삭제하고 저장할 수 있다 (페이지를 덮어쓴다)")
    void testSaveOverwritesPages() {
        // Given - 3개의 페이지로 저장
        List<BookPage> initialPages = Arrays.asList(
                new BookPage("페이지 1", "url1", 1),
                new BookPage("페이지 2", "url2", 2),
                new BookPage("페이지 3", "url3", 3)
        );
        BookInProgress bookWithPages = new BookInProgress(
                "book-update-test",
                1L,
                "업데이트 테스트",
                testCharacter,
                initialPages,
                BookInProgress.Status.IN_PROGRESS

        );
        adapter.save(bookWithPages);

        // When - 1개의 페이지로 업데이트
        List<BookPage> updatedPages = List.of(
                new BookPage("새로운 페이지 1", "new-url1", 1)
        );
        BookInProgress updatedBook = new BookInProgress(
                "book-update-test",
                1L,
                "업데이트 테스트",
                testCharacter,
                updatedPages,
                BookInProgress.Status.IN_PROGRESS

        );
        adapter.save(updatedBook);

        // Then
        BookInProgress retrieved = adapter.retrieveById("book-update-test");
        assertThat(retrieved.previousPages()).hasSize(1);
        assertThat(retrieved.previousPages().get(0).context()).isEqualTo("새로운 페이지 1");

        System.out.println("save: Pages overwritten successfully");
    }

    @Test
    @DisplayName("save: 캐릭터 정보가 올바르게 저장된다")
    void testSaveCharacterInformation() {
        // Given
        BookCharacter detailedCharacter = new BookCharacter(
                200L,
                2L,
                "사자 레오",
                "황금빛 갈기, 위엄있는 모습",
                "용감하고 정의로운 성격",
                "숲의 왕",
                "http://example.com/lion.png"
        );

        BookInProgress book = new BookInProgress(
                "test-book-002",
                2L,
                "사자의 모험",
                detailedCharacter,
                Collections.emptyList(),
                BookInProgress.Status.IN_PROGRESS

        );

        // When
        adapter.save(book);

        // Then
        BookInProgress retrieved = adapter.retrieveById("test-book-002");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.character().id()).isEqualTo(200L);
        assertThat(retrieved.character().name()).isEqualTo("사자 레오");
        assertThat(retrieved.character().appearanceKeywords()).isEqualTo("황금빛 갈기, 위엄있는 모습");
        assertThat(retrieved.character().personality()).isEqualTo("용감하고 정의로운 성격");
        assertThat(retrieved.character().description()).isEqualTo("숲의 왕");
        assertThat(retrieved.character().imageUrl()).isEqualTo("http://example.com/lion.png");

        System.out.println("save: Character information saved correctly");
    }

    // ==================== retrieveById() 메서드 테스트 ====================

    @Test
    @DisplayName("retrieveById: 저장된 BookInProgress를 ID로 조회할 수 있다")
    void testRetrieveById() {
        // Given
        adapter.save(testBookInProgress);

        // When
        BookInProgress retrieved = adapter.retrieveById("test-book-001");

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo("test-book-001");
        assertThat(retrieved.ownerId()).isEqualTo(1L);
        assertThat(retrieved.backgroundInfo()).isEqualTo("숲속 친구들의 모험 이야기");
        assertThat(retrieved.character().name()).isEqualTo("토끼 토리");
        assertThat(retrieved.previousPages()).isEmpty();

        System.out.println("retrieveById: BookInProgress retrieved successfully");
    }

    @Test
    @DisplayName("retrieveById: 페이지가 있는 BookInProgress를 조회할 수 있다")
    void testRetrieveByIdWithPages() {
        // Given
        List<BookPage> pages = Arrays.asList(
                new BookPage("페이지 1", "url1", 1),
                new BookPage("페이지 2", "url2", 2)
        );
        BookInProgress bookWithPages = new BookInProgress(
                "book-multiple-pages",
                1L,
                "여러 페이지 테스트",
                testCharacter,
                pages,
                BookInProgress.Status.IN_PROGRESS

        );
        adapter.save(bookWithPages);

        // When
        BookInProgress retrieved = adapter.retrieveById("book-multiple-pages");

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.previousPages()).hasSize(2);
        assertThat(retrieved.previousPages().get(0).context()).isEqualTo("페이지 1");
        assertThat(retrieved.previousPages().get(0).pageNumber()).isEqualTo(1);
        assertThat(retrieved.previousPages().get(1).context()).isEqualTo("페이지 2");
        assertThat(retrieved.previousPages().get(1).pageNumber()).isEqualTo(2);

        System.out.println("retrieveById: BookInProgress with pages retrieved successfully");
    }

    @Test
    @DisplayName("retrieveById: 존재하지 않는 ID로 조회 시 null을 반환한다")
    void testRetrieveByIdNotFound() {
        // When
        BookInProgress retrieved = adapter.retrieveById("non-existent-id");

        // Then
        assertThat(retrieved).isNull();

        System.out.println("retrieveById: Null returned for non-existent ID");
    }

    @Test
    @DisplayName("retrieveById: 페이지가 없는 BookInProgress는 빈 리스트를 반환한다")
    void testRetrieveByIdWithNoPages() {
        // Given
        adapter.save(testBookInProgress);

        // When
        BookInProgress retrieved = adapter.retrieveById("test-book-001");

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.previousPages()).isNotNull();
        assertThat(retrieved.previousPages()).isEmpty();

        System.out.println("retrieveById: Empty page list returned correctly");
    }

    // ==================== retrieveByMemberId() 메서드 테스트 ====================

    @Test
    @DisplayName("retrieveByMemberId: 특정 회원의 모든 BookInProgress를 조회할 수 있다")
    void testRetrieveByMemberId() {
        // Given
        BookInProgress book1 = new BookInProgress(
                "test-book-001",
                1L,
                "첫 번째 책",
                testCharacter,
                Collections.emptyList(),
                BookInProgress.Status.IN_PROGRESS

        );
        BookInProgress book2 = new BookInProgress(
                "test-book-002",
                1L,
                "두 번째 책",
                testCharacter,
                Collections.emptyList(),
                BookInProgress.Status.IN_PROGRESS

        );

        adapter.save(book1);
        adapter.save(book2);

        // When
        List<BookInProgress> retrieved = adapter.retrieveByMemberId(1L);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).hasSize(2);
        assertThat(retrieved).extracting(BookInProgress::id)
                .containsExactlyInAnyOrder("test-book-001", "test-book-002");
        assertThat(retrieved).allMatch(book -> book.ownerId().equals(1L));

        System.out.println("retrieveByMemberId: All books for member retrieved successfully");
    }

    @Test
    @DisplayName("retrieveByMemberId: 여러 회원의 책을 독립적으로 조회할 수 있다")
    void testRetrieveByMemberIdMultipleMembers() {
        // Given
        BookInProgress member1Book = new BookInProgress(
                "test-book-001",
                1L,
                "회원1의 책",
                testCharacter,
                Collections.emptyList(),
                BookInProgress.Status.IN_PROGRESS
        );

        BookCharacter member2Character = new BookCharacter(
                200L, 2L, "다람쥐 다이", "갈색 털", "활발한", "다람쁘", "url"
        );
        BookInProgress member2Book = new BookInProgress(
                "test-book-002",
                2L,
                "회원2의 책",
                member2Character,
                Collections.emptyList(),
                BookInProgress.Status.IN_PROGRESS

        );

        adapter.save(member1Book);
        adapter.save(member2Book);

        // When
        List<BookInProgress> member1Books = adapter.retrieveByMemberId(1L);
        List<BookInProgress> member2Books = adapter.retrieveByMemberId(2L);

        // Then
        assertThat(member1Books).hasSize(1);
        assertThat(member1Books.get(0).id()).isEqualTo("test-book-001");
        assertThat(member1Books.get(0).ownerId()).isEqualTo(1L);

        assertThat(member2Books).hasSize(1);
        assertThat(member2Books.get(0).id()).isEqualTo("test-book-002");
        assertThat(member2Books.get(0).ownerId()).isEqualTo(2L);

        System.out.println("retrieveByMemberId: Multiple members' books retrieved independently");
    }

    @Test
    @DisplayName("retrieveByMemberId: 존재하지 않는 회원 ID로 조회 시 빈 리스트를 반환한다")
    void testRetrieveByMemberIdNotFound() {
        // When
        List<BookInProgress> retrieved = adapter.retrieveByMemberId(999L);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).isEmpty();

        System.out.println("retrieveByMemberId: Empty list returned for non-existent member");
    }

    @Test
    @DisplayName("retrieveByMemberId: 페이지가 있는 책들도 함께 조회된다")
    void testRetrieveByMemberIdWithPages() {
        // Given
        List<BookPage> pages1 = List.of(
                new BookPage("책1 페이지1", "url1", 1)
        );
        List<BookPage> pages2 = Arrays.asList(
                new BookPage("책2 페이지1", "url2-1", 1),
                new BookPage("책2 페이지2", "url2-2", 2)
        );

        BookInProgress book1 = new BookInProgress(
                "test-book-001",
                1L,
                "첫 번째 책",
                testCharacter,
                pages1,
                BookInProgress.Status.IN_PROGRESS
        );
        BookInProgress book2 = new BookInProgress(
                "test-book-002",
                1L,
                "두 번째 책",
                testCharacter,
                pages2,
                BookInProgress.Status.IN_PROGRESS

        );

        adapter.save(book1);
        adapter.save(book2);

        // When
        List<BookInProgress> retrieved = adapter.retrieveByMemberId(1L);

        // Then
        assertThat(retrieved).hasSize(2);

        BookInProgress retrievedBook1 = retrieved.stream()
                .filter(book -> book.id().equals("test-book-001"))
                .findFirst()
                .orElse(null);
        assertThat(retrievedBook1).isNotNull();
        assertThat(retrievedBook1.previousPages()).hasSize(1);

        BookInProgress retrievedBook2 = retrieved.stream()
                .filter(book -> book.id().equals("test-book-002"))
                .findFirst()
                .orElse(null);
        assertThat(retrievedBook2).isNotNull();
        assertThat(retrievedBook2.previousPages()).hasSize(2);

        System.out.println("retrieveByMemberId: Books with pages retrieved successfully");
    }

    // ==================== 통합 시나리오 테스트 ====================

    @Test
    @DisplayName("통합: save -> retrieveById -> update -> retrieveById 시나리오")
    void testCompleteWorkflow() {
        // 1. 초기 저장
        BookInProgress saved = adapter.save(testBookInProgress);
        assertThat(saved.previousPages()).isEmpty();

        // 2. ID로 조회
        BookInProgress retrieved1 = adapter.retrieveById("test-book-001");
        assertThat(retrieved1).isNotNull();
        assertThat(retrieved1.previousPages()).isEmpty();

        // 3. 페이지 추가 및 업데이트
        BookPage page1 = new BookPage("페이지 1", "url1", 1);
        BookInProgress updated = retrieved1.addBookPage(page1);
        adapter.save(updated);

        // 4. 다시 조회하여 확인
        BookInProgress retrieved2 = adapter.retrieveById("test-book-001");
        assertThat(retrieved2.previousPages()).hasSize(1);
        assertThat(retrieved2.previousPages().get(0).context()).isEqualTo("페이지 1");

        // 5. 또 다른 페이지 추가
        BookPage page2 = new BookPage("페이지 2", "url2", 2);
        BookInProgress updated2 = retrieved2.addBookPage(page2);
        adapter.save(updated2);

        // 6. 최종 조회
        BookInProgress retrieved3 = adapter.retrieveById("test-book-001");
        assertThat(retrieved3.previousPages()).hasSize(2);
        assertThat(retrieved3.previousPages().get(0).context()).isEqualTo("페이지 1");
        assertThat(retrieved3.previousPages().get(1).context()).isEqualTo("페이지 2");

        System.out.println("통합: Complete workflow executed successfully");
    }

    @Test
    @DisplayName("통합: 여러 회원의 책을 저장하고 각각 조회할 수 있다")
    void testMultipleMembersIntegration() {
        // Given - 3명의 회원이 각각 책을 생성
        for (long memberId = 1; memberId <= 3; memberId++) {
            BookCharacter character = new BookCharacter(
                    memberId * 100,
                    memberId,
                    "캐릭터" + memberId,
                    "외모" + memberId,
                    "성격" + memberId,
                    "설명" + memberId,
                    "url" + memberId
            );

            BookInProgress book = new BookInProgress(
                    "book-member" + memberId,
                    memberId,
                    "회원" + memberId + "의 책",
                    character,
                    Collections.emptyList(),
                    BookInProgress.Status.IN_PROGRESS
            );

            adapter.save(book);
        }

        // When & Then - 각 회원의 책을 독립적으로 조회
        for (long memberId = 1; memberId <= 3; memberId++) {
            List<BookInProgress> books = adapter.retrieveByMemberId(memberId);
            assertThat(books).hasSize(1);
            assertThat(books.get(0).ownerId()).isEqualTo(memberId);
            assertThat(books.get(0).backgroundInfo()).isEqualTo("회원" + memberId + "의 책");

            // Clean up for this test
            redisRepository.delete("book-member" + memberId);
        }

        System.out.println("통합: Multiple members integration test passed");
    }

    // ==================== PENDING 상태 테스트 ====================

    @Test
    @DisplayName("Status.fromDomain: PENDING 도메인 상태를 Redis PENDING으로 변환한다")
    void testFromDomainPending() {
        BookInProgressRedisEntity.Status result = BookInProgressRedisEntity.Status.fromDomain(BookInProgress.Status.PENDING);
        assertThat(result).isEqualTo(BookInProgressRedisEntity.Status.PENDING);
    }

    @Test
    @DisplayName("Status.toDomain: Redis PENDING을 도메인 PENDING으로 변환한다")
    void testToDomainPending() {
        BookInProgress.Status result = BookInProgressRedisEntity.Status.toDomain(BookInProgressRedisEntity.Status.PENDING);
        assertThat(result).isEqualTo(BookInProgress.Status.PENDING);
    }

    @Test
    @DisplayName("save/retrieveById: PENDING 상태를 저장하고 조회하면 PENDING이 유지된다")
    void testSaveAndRetrievePendingStatus() {
        // Given
        BookInProgress pending = new BookInProgress(
                "test-book-003",
                1L,
                "PENDING 테스트",
                testCharacter,
                Collections.emptyList(),
                BookInProgress.Status.PENDING
        );

        // When
        adapter.save(pending);
        BookInProgress retrieved = adapter.retrieveById("test-book-003");

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.status()).isEqualTo(BookInProgress.Status.PENDING);
    }

    // ==================== markAsCompleted() 메서드 테스트 ====================

    @Test
    @DisplayName("markAsCompleted: IN_PROGRESS 상태를 COMPLETED로 변경한다")
    void testMarkAsCompleted() {
        // Given
        adapter.save(testBookInProgress);

        // When
        adapter.markAsCompleted("test-book-001");

        // Then
        BookInProgress retrieved = adapter.retrieveById("test-book-001");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.status()).isEqualTo(BookInProgress.Status.COMPLETED);
    }

    @Test
    @DisplayName("markAsCompleted: 존재하지 않는 ID는 예외 없이 무시된다")
    void testMarkAsCompletedNonExistent() {
        // When & Then - should not throw
        assertThatCode(() -> adapter.markAsCompleted("non-existent-id"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("markAsCompleted: 완료 후 기존 메타데이터가 유지된다")
    void testMarkAsCompletedPreservesMetadata() {
        // Given
        adapter.save(testBookInProgress);

        // When
        adapter.markAsCompleted("test-book-001");

        // Then
        BookInProgress retrieved = adapter.retrieveById("test-book-001");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.status()).isEqualTo(BookInProgress.Status.COMPLETED);
        assertThat(retrieved.id()).isEqualTo("test-book-001");
        assertThat(retrieved.ownerId()).isEqualTo(1L);
        assertThat(retrieved.backgroundInfo()).isEqualTo("숲속 친구들의 모험 이야기");
        assertThat(retrieved.character().name()).isEqualTo("토끼 토리");
    }
}
