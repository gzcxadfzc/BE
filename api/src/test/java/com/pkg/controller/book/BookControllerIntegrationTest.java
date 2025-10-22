package com.pkg.controller.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pkg.TestApplication;
import com.pkg.authentication.token.AccessToken;
import com.pkg.authentication.token.AccessTokenAuthenticator;
import com.pkg.authentication.token.MemberPrincipal;
import com.pkg.domain.member.Role;
import com.pkg.jpa.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@ActiveProfiles("test-in-memory")
@DisplayName("BookController 통합 테스트")
class BookControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @MockBean
    private AccessTokenAuthenticator mockTokenAuthenticator;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookJpaRepository bookJpaRepository;

    @Autowired
    private CharacterJpaRepository characterJpaRepository;

    @Autowired
    private PageJpaRepository pageJpaRepository;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private Long testMemberId;
    private Long testCharacterId;
    private String testBookId;

    @BeforeEach
    void setUp() {
        // Clean up database
        pageJpaRepository.deleteAll();
        bookJpaRepository.deleteAll();
        characterJpaRepository.deleteAll();
        memberJpaRepository.deleteAll();
        entityManager.clear();

        // Insert test member
        MemberJpaEntity member = MemberJpaEntity.builder()
                .username("testuser")
                .password("password123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        memberJpaRepository.save(member);
        testMemberId = member.getId();

        // Insert test character
        CharacterJpaEntity character = CharacterJpaEntity.builder()
                .memberId(testMemberId)
                .name("Alice")
                .appearanceKeywords("blonde, blue eyes")
                .personality("curious and brave")
                .userDescription("A young girl who loves adventure")
                .imageUrl("https://example.com/character.jpg")
                .originImageUrl("https://example.com/origin.jpg")
                .build();
        characterJpaRepository.save(character);
        testCharacterId = character.getId();

        // Insert test book
        BookJpaEntity book = BookJpaEntity.builder()
                .id("test-book-123")
                .userId(testMemberId)
                .characterId(testCharacterId)
                .title("Alice's Adventure")
                .author("John Doe")
                .createdAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .bookColor(1L)
                .storyLength(2)
                .coverImageUrl("https://example.com/cover.jpg")
                .build();
        bookJpaRepository.save(book);
        testBookId = book.getId();

        // Insert test pages
        PageJpaEntity page1 = PageJpaEntity.builder()
                .bookId(testBookId)
                .context("Once upon a time...")
                .imageUrl("https://example.com/page1.jpg")
                .pageNumber(1)
                .build();
        pageJpaRepository.save(page1);

        PageJpaEntity page2 = PageJpaEntity.builder()
                .bookId(testBookId)
                .context("The adventure begins...")
                .imageUrl("https://example.com/page2.jpg")
                .pageNumber(2)
                .build();
        pageJpaRepository.save(page2);
    }

    @Test
    @DisplayName("GET /api/v1/book/board/{bookId} - 책 ID로 책 상세 정보를 조회할 수 있다")
    void getBookById_shouldReturnBookDetails() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/book/board/{bookId}", testBookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(testBookId))
                .andExpect(jsonPath("$.data.memberId").value(testMemberId))
                .andExpect(jsonPath("$.data.title").value("Alice's Adventure"))
                .andExpect(jsonPath("$.data.author").value("John Doe"))
                .andExpect(jsonPath("$.data.bookPages", hasSize(2)))
                .andExpect(jsonPath("$.data.bookPages[0].context").value("Once upon a time..."))
                .andExpect(jsonPath("$.data.bookPages[0].imageUrl").value("https://example.com/page1.jpg"))
                .andExpect(jsonPath("$.data.bookPages[0].pageNumber").value(1))
                .andExpect(jsonPath("$.data.bookPages[1].context").value("The adventure begins..."))
                .andExpect(jsonPath("$.data.bookPages[1].imageUrl").value("https://example.com/page2.jpg"))
                .andExpect(jsonPath("$.data.bookPages[1].pageNumber").value(2))
                .andExpect(jsonPath("$.data.character.id").value(testCharacterId))
                .andExpect(jsonPath("$.data.character.name").value("Alice"))
                .andExpect(jsonPath("$.data.character.personality").value("curious and brave"))
                .andExpect(jsonPath("$.data.character.description").value("A young girl who loves adventure"))
                .andExpect(jsonPath("$.data.character.imageUrl").value("https://example.com/character.jpg"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/board/{bookId} - 존재하지 않는 책 ID로 조회 시 예외가 발생한다")
    void getBookById_shouldThrowException_whenBookNotFound() throws Exception {
        // given
        String nonExistentBookId = "non-existent-book-id";

        // when & then
        mockMvc.perform(get("/api/v1/book/board/{bookId}", nonExistentBookId))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/my - 인증된 사용자의 책 썸네일 목록을 조회할 수 있다")
    void getMyBooks_shouldReturnBookThumbnails() throws Exception {
        // given - 추가 책 생성
        CharacterJpaEntity character2 = CharacterJpaEntity.builder()
                .memberId(testMemberId)
                .name("Bob")
                .appearanceKeywords("tall, dark hair")
                .personality("shy and thoughtful")
                .userDescription("A quiet observer")
                .imageUrl("https://example.com/bob.jpg")
                .originImageUrl("https://example.com/bob-origin.jpg")
                .build();
        characterJpaRepository.save(character2);

        BookJpaEntity book2 = BookJpaEntity.builder()
                .id("test-book-456")
                .userId(testMemberId)
                .characterId(character2.getId())
                .title("Bob's Journey")
                .author("Jane Smith")
                .bookColor(2L)
                .storyLength(3)
                .coverImageUrl("https://example.com/cover2.jpg")
                .build();
        bookJpaRepository.save(book2);

        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(testMemberId, Role.MEMBER));

        // when & then
        mockMvc.perform(
                get("/api/v1/book/my").header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.thumbnails", hasSize(2)))
                .andExpect(jsonPath("$.data.thumbnails[0].bookId").value("test-book-123"))
                .andExpect(jsonPath("$.data.thumbnails[0].title").value("Alice's Adventure"))
                .andExpect(jsonPath("$.data.thumbnails[0].author").value("John Doe"))
                .andExpect(jsonPath("$.data.thumbnails[0].imageUrl").value("https://example.com/cover.jpg"))
                .andExpect(jsonPath("$.data.thumbnails[0].createdAt").exists())
                .andExpect(jsonPath("$.data.thumbnails[1].bookId").value("test-book-456"))
                .andExpect(jsonPath("$.data.thumbnails[1].title").value("Bob's Journey"))
                .andExpect(jsonPath("$.data.thumbnails[1].author").value("Jane Smith"))
                .andExpect(jsonPath("$.data.thumbnails[1].imageUrl").value("https://example.com/cover2.jpg"))
                .andExpect(jsonPath("$.data.thumbnails[1].createdAt").exists())
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/my - 책이 없는 사용자는 빈 배열을 받는다")
    void getMyBooks_shouldReturnEmptyList_whenUserHasNoBooks() throws Exception {
        // given - 새로운 사용자 생성 (책 없음)
        MemberJpaEntity newMember = MemberJpaEntity.builder()
                .username("newuser")
                .password("newpass123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();

        MemberJpaEntity entity = memberJpaRepository.save(newMember);

        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(entity.getId(), Role.MEMBER));

        // when & then
        mockMvc.perform(get("/api/v1/book/my").header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.thumbnails", hasSize(0)))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/board/{bookId} - 여러 페이지를 가진 책을 페이지 순서대로 조회할 수 있다")
    void getBookById_shouldReturnPagesInOrder_whenBookHasMultiplePages() throws Exception {
        // given - 페이지 3개 더 추가
        PageJpaEntity page3 = PageJpaEntity.builder()
                .bookId(testBookId)
                .context("Page 3 content")
                .imageUrl("https://example.com/page3.jpg")
                .pageNumber(3)
                .build();
        pageJpaRepository.save(page3);

        PageJpaEntity page4 = PageJpaEntity.builder()
                .bookId(testBookId)
                .context("Page 4 content")
                .imageUrl("https://example.com/page4.jpg")
                .pageNumber(4)
                .build();
        pageJpaRepository.save(page4);

        PageJpaEntity page5 = PageJpaEntity.builder()
                .bookId(testBookId)
                .context("Page 5 content")
                .imageUrl("https://example.com/page5.jpg")
                .pageNumber(5)
                .build();
        pageJpaRepository.save(page5);

        // when & then
        mockMvc.perform(get("/api/v1/book/board/{bookId}", testBookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookPages", hasSize(5)))
                .andExpect(jsonPath("$.data.bookPages[0].pageNumber").value(1))
                .andExpect(jsonPath("$.data.bookPages[0].context").value("Once upon a time..."))
                .andExpect(jsonPath("$.data.bookPages[1].pageNumber").value(2))
                .andExpect(jsonPath("$.data.bookPages[1].context").value("The adventure begins..."))
                .andExpect(jsonPath("$.data.bookPages[2].pageNumber").value(3))
                .andExpect(jsonPath("$.data.bookPages[2].context").value("Page 3 content"))
                .andExpect(jsonPath("$.data.bookPages[3].pageNumber").value(4))
                .andExpect(jsonPath("$.data.bookPages[3].context").value("Page 4 content"))
                .andExpect(jsonPath("$.data.bookPages[4].pageNumber").value(5))
                .andExpect(jsonPath("$.data.bookPages[4].context").value("Page 5 content"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/my - 다른 사용자의 책은 조회되지 않는다")
    void getMyBooks_shouldNotReturnOtherUsersBooks() throws Exception {
        // given - 다른 사용자와 그 사용자의 책 생성
        MemberJpaEntity otherMember = MemberJpaEntity.builder()
                .username("otheruser")
                .password("otherpass123")
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
        memberJpaRepository.save(otherMember);

        CharacterJpaEntity otherCharacter = CharacterJpaEntity.builder()
                .memberId(otherMember.getId())
                .name("Other Character")
                .appearanceKeywords("big, strong")
                .personality("brave")
                .userDescription("A strong character")
                .imageUrl("https://example.com/other.jpg")
                .originImageUrl("https://example.com/other-origin.jpg")
                .build();
        characterJpaRepository.save(otherCharacter);

        BookJpaEntity otherBook = BookJpaEntity.builder()
                .id("other-book-789")
                .userId(otherMember.getId())
                .characterId(otherCharacter.getId())
                .title("Other's Book")
                .author("Other Author")
                .createdAt(LocalDateTime.now())
                .bookColor(3L)
                .storyLength(1)
                .coverImageUrl("https://example.com/other-cover.jpg")
                .build();
        bookJpaRepository.save(otherBook);

        when(mockTokenAuthenticator.authenticate(any(AccessToken.class)))
                .thenReturn(new MemberPrincipal(testMemberId, Role.MEMBER));

        // when & then - testMemberId로 조회하면 otherMember의 책은 안 나와야 함
        mockMvc.perform(get("/api/v1/book/my").header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.thumbnails", hasSize(1)))
                .andExpect(jsonPath("$.data.thumbnails[0].bookId").value("test-book-123"))
                .andExpect(jsonPath("$.data.thumbnails[0].title").value("Alice's Adventure"))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /api/v1/book/board/{bookId} - 페이지가 없는 책도 조회할 수 있다")
    void getBookById_shouldReturnBook_whenBookHasNoPages() throws Exception {
        // given - 페이지가 없는 책 생성
        BookJpaEntity emptyBook = BookJpaEntity.builder()
                .id("empty-book")
                .userId(testMemberId)
                .characterId(testCharacterId)
                .title("Empty Book")
                .author("Empty Author")
                .createdAt(LocalDateTime.now())
                .bookColor(1L)
                .storyLength(0)
                .coverImageUrl("https://example.com/empty-cover.jpg")
                .build();
        bookJpaRepository.save(emptyBook);

        // when & then
        mockMvc.perform(get("/api/v1/book/board/{bookId}", "empty-book"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value("empty-book"))
                .andExpect(jsonPath("$.data.title").value("Empty Book"))
                .andExpect(jsonPath("$.data.bookPages", hasSize(0)))
                .andDo(print());
    }
}
