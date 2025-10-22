package com.pkg.jpa;

import com.pkg.domain.book.Book;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.book.BookThumbnail;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.member.Actor;
import com.pkg.domain.member.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test-in-memory")
@Import(BookRepositoryAdapter.class)
class BookRepositoryAdapterTest {

    @Autowired
    private BookRepositoryAdapter bookRepositoryAdapter;

    @Autowired
    private BookJpaRepository bookJpaRepository;

    @Autowired
    private CharacterJpaRepository characterJpaRepository;

    @Autowired
    private PageJpaRepository pageJpaRepository;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

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

        // Insert test member
        MemberJpaEntity member = createMember("testuser", "password123");
        memberJpaRepository.save(member);
        testMemberId = member.getId();

        // Insert test character
        CharacterJpaEntity character = createCharacter(
                testMemberId,
                "Test Character",
                "tall, brave",
                "adventurous",
                "A brave adventurer",
                "http://example.com/character.jpg",
                "http://example.com/origin.jpg"
        );
        characterJpaRepository.save(character);
        testCharacterId = character.getId();

        // Insert test book
        BookJpaEntity book = BookJpaEntity.builder()
                .id("book-123")
                .userId(testMemberId)
                .characterId(testCharacterId)
                .title("Test Book")
                .author("Test Author")
                .createdAt(LocalDateTime.now())
                .bookColor(1L)
                .storyLength(10)
                .coverImageUrl("http://example.com/cover.jpg")
                .build();
        bookJpaRepository.save(book);
        testBookId = book.getId();

        // Insert test pages
        pageJpaRepository.save(createPage(testBookId, "Page 1 content", "http://example.com/page1.jpg", 1));
        pageJpaRepository.save(createPage(testBookId, "Page 2 content", "http://example.com/page2.jpg", 2));
    }

    @Test
    @DisplayName("save - should save new book with pages to H2 database")
    void save() {
        // Given
        String newBookId = "new-book-789";
        BookCharacter testCharacter = new BookCharacter(
                testCharacterId,
                testMemberId,
                "Test Character",
                "tall, brave",
                "adventurous",
                "A brave adventurer",
                "http://example.com/character.jpg"
        );

        List<BookPage> bookPages = List.of(
                new com.pkg.domain.book.BookPage("New page 1 content", "http://example.com/new-page1.jpg", 1),
                new com.pkg.domain.book.BookPage("New page 2 content", "http://example.com/new-page2.jpg", 2),
                new com.pkg.domain.book.BookPage("New page 3 content", "http://example.com/new-page3.jpg", 3)
        );

        Book newBook = Book.builder()
                .id(newBookId)
                .memberId(testMemberId)
                .title("New Book Title")
                .author("New Author")
                .character(testCharacter)
                .bookPages(bookPages)
                .build();

        // When
        Book savedBook = bookRepositoryAdapter.save(newBook);

        // Then - verify returned book
        assertThat(savedBook).isNotNull();
        assertThat(savedBook.id()).isEqualTo(newBookId);
        assertThat(savedBook.title()).isEqualTo("New Book Title");
        assertThat(savedBook.author()).isEqualTo("New Author");
        assertThat(savedBook.memberId()).isEqualTo(testMemberId);
        assertThat(savedBook.bookPages()).hasSize(3);

        // Verify book entity was persisted
        BookJpaEntity savedBookEntity = bookJpaRepository.findById(newBookId).orElse(null);
        assertThat(savedBookEntity).isNotNull();
        assertThat(savedBookEntity.getId()).isEqualTo(newBookId);
        assertThat(savedBookEntity.getTitle()).isEqualTo("New Book Title");
        assertThat(savedBookEntity.getAuthor()).isEqualTo("New Author");
        assertThat(savedBookEntity.getUserId()).isEqualTo(testMemberId);
        assertThat(savedBookEntity.getCharacterId()).isEqualTo(testCharacterId);
        assertThat(savedBookEntity.getCoverImageUrl()).isEqualTo("http://example.com/new-page1.jpg");
        assertThat(savedBookEntity.getStoryLength()).isEqualTo(3);
        assertThat(savedBookEntity.getBookColor()).isEqualTo(1L);
        assertThat(savedBookEntity.getCreatedAt()).isNotNull();

        // Verify pages were persisted
        List<PageJpaEntity> savedPages = pageJpaRepository.findAllByBookId(newBookId);
        assertThat(savedPages).hasSize(3);
        assertThat(savedPages).extracting("context")
                .containsExactly("New page 1 content", "New page 2 content", "New page 3 content");
        assertThat(savedPages).extracting("image_url")
                .containsExactly("http://example.com/new-page1.jpg",
                                "http://example.com/new-page2.jpg",
                                "http://example.com/new-page3.jpg");
        assertThat(savedPages).extracting("pageNumber")
                .containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("save - should save book with single page")
    void save_withSinglePage() {
        // Given
        String bookId = "single-page-book";
        BookCharacter testCharacter = new BookCharacter(
                testCharacterId,
                testMemberId,
                "Test Character",
                "tall, brave",
                "adventurous",
                "A brave adventurer",
                "http://example.com/character.jpg"
        );

        List<com.pkg.domain.book.BookPage> bookPages = List.of(
                new com.pkg.domain.book.BookPage("Single page content", "http://example.com/single-page.jpg", 1)
        );

        Book newBook = Book.builder()
                .id(bookId)
                .memberId(testMemberId)
                .title("Single Page Book")
                .author("Single Author")
                .character(testCharacter)
                .bookPages(bookPages)
                .build();

        // When
        Book savedBook = bookRepositoryAdapter.save(newBook);

        // Then
        assertThat(savedBook).isNotNull();
        assertThat(savedBook.bookPages()).hasSize(1);
        assertThat(savedBook.bookPages().get(0).context()).isEqualTo("Single page content");

        // Verify in database
        List<PageJpaEntity> savedPages = pageJpaRepository.findAllByBookId(bookId);
        assertThat(savedPages).hasSize(1);
        assertThat(savedPages.get(0).toBookPage().context()).isEqualTo("Single page content");
    }

    @Test
    @DisplayName("save - should set cover image from first page")
    void save_shouldSetCoverImageFromFirstPage() {
        // Given
        String bookId = "cover-test-book";
        BookCharacter testCharacter = new BookCharacter(
                testCharacterId,
                testMemberId,
                "Test Character",
                "tall, brave",
                "adventurous",
                "A brave adventurer",
                "http://example.com/character.jpg"
        );

        List<com.pkg.domain.book.BookPage> bookPages = List.of(
                new com.pkg.domain.book.BookPage("Page 1", "http://example.com/first-page.jpg", 1),
                new com.pkg.domain.book.BookPage("Page 2", "http://example.com/second-page.jpg", 2)
        );

        Book newBook = Book.builder()
                .id(bookId)
                .memberId(testMemberId)
                .title("Cover Test Book")
                .author("Cover Author")
                .character(testCharacter)
                .bookPages(bookPages)
                .build();

        // When
        bookRepositoryAdapter.save(newBook);

        // Then - cover image should be from first page
        BookJpaEntity savedBookEntity = bookJpaRepository.findById(bookId).orElse(null);
        assertThat(savedBookEntity).isNotNull();
        assertThat(savedBookEntity.getCoverImageUrl()).isEqualTo("http://example.com/first-page.jpg");
    }

    @Test
    @DisplayName("save - should set story length based on number of pages")
    void save_shouldSetStoryLengthCorrectly() {
        // Given
        String bookId = "length-test-book";
        BookCharacter testCharacter = new BookCharacter(
                testCharacterId,
                testMemberId,
                "Test Character",
                "tall, brave",
                "adventurous",
                "A brave adventurer",
                "http://example.com/character.jpg"
        );

        List<com.pkg.domain.book.BookPage> bookPages = List.of(
                new com.pkg.domain.book.BookPage("Page 1", "http://example.com/page1.jpg", 1),
                new com.pkg.domain.book.BookPage("Page 2", "http://example.com/page2.jpg", 2),
                new com.pkg.domain.book.BookPage("Page 3", "http://example.com/page3.jpg", 3),
                new com.pkg.domain.book.BookPage("Page 4", "http://example.com/page4.jpg", 4),
                new com.pkg.domain.book.BookPage("Page 5", "http://example.com/page5.jpg", 5)
        );

        Book newBook = Book.builder()
                .id(bookId)
                .memberId(testMemberId)
                .title("Length Test Book")
                .author("Length Author")
                .character(testCharacter)
                .bookPages(bookPages)
                .build();

        // When
        bookRepositoryAdapter.save(newBook);

        // Then - story length should match number of pages
        BookJpaEntity savedBookEntity = bookJpaRepository.findById(bookId).orElse(null);
        assertThat(savedBookEntity).isNotNull();
        assertThat(savedBookEntity.getStoryLength()).isEqualTo(5);
    }

    @Test
    @DisplayName("save - should return complete book with all relationships")
    void save_shouldReturnCompleteBook() {
        // Given
        String bookId = "complete-test-book";
        BookCharacter testCharacter = new BookCharacter(
                testCharacterId,
                testMemberId,
                "Test Character",
                "tall, brave",
                "adventurous",
                "A brave adventurer",
                "http://example.com/character.jpg"
        );

        List<BookPage> bookPages = List.of(
                new com.pkg.domain.book.BookPage("Content 1", "http://example.com/img1.jpg", 1),
                new com.pkg.domain.book.BookPage("Content 2", "http://example.com/img2.jpg", 2)
        );

        Book newBook = Book.builder()
                .id(bookId)
                .memberId(testMemberId)
                .title("Complete Test")
                .author("Complete Author")
                .character(testCharacter)
                .bookPages(bookPages)
                .build();

        // When
        Book savedBook = bookRepositoryAdapter.save(newBook);

        // Then - verify complete book structure
        assertThat(savedBook).isNotNull();
        assertThat(savedBook.id()).isEqualTo(bookId);
        assertThat(savedBook.title()).isEqualTo("Complete Test");
        assertThat(savedBook.author()).isEqualTo("Complete Author");
        assertThat(savedBook.memberId()).isEqualTo(testMemberId);

        // Verify character relationship
        assertThat(savedBook.character()).isNotNull();
        assertThat(savedBook.character().id()).isEqualTo(testCharacterId);
        assertThat(savedBook.character().name()).isEqualTo("Test Character");

        // Verify pages
        assertThat(savedBook.bookPages()).hasSize(2);
        assertThat(savedBook.bookPages().get(0).context()).isEqualTo("Content 1");
        assertThat(savedBook.bookPages().get(1).context()).isEqualTo("Content 2");
    }

    @Test
    @DisplayName("retrieveById - should retrieve book with character and pages from H2 database")
    void retrieveById() {
        // When
        Book result = bookRepositoryAdapter.retrieveById(testBookId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("book-123");
        assertThat(result.title()).isEqualTo("Test Book");
        assertThat(result.author()).isEqualTo("Test Author");
        assertThat(result.memberId()).isEqualTo(testMemberId);

        // Verify character
        assertThat(result.character()).isNotNull();
        assertThat(result.character().id()).isEqualTo(testCharacterId);
        assertThat(result.character().name()).isEqualTo("Test Character");
        assertThat(result.character().appearanceKeywords()).isEqualTo("tall, brave");
        assertThat(result.character().personality()).isEqualTo("adventurous");
        assertThat(result.character().description()).isEqualTo("A brave adventurer");
        assertThat(result.character().imageUrl()).isEqualTo("http://example.com/character.jpg");

        // Verify pages
        assertThat(result.bookPages()).hasSize(2);
        assertThat(result.bookPages().get(0).context()).isEqualTo("Page 1 content");
        assertThat(result.bookPages().get(0).imageUrl()).isEqualTo("http://example.com/page1.jpg");
        assertThat(result.bookPages().get(1).context()).isEqualTo("Page 2 content");
        assertThat(result.bookPages().get(1).imageUrl()).isEqualTo("http://example.com/page2.jpg");
    }

    @Test
    @DisplayName("retrieveThumbnailsByUser - should retrieve all thumbnails for a user from H2 database")
    void retrieveThumbnailsByUser() {
        // Given - create second character and book for the same user
        CharacterJpaEntity character2 = createCharacter(
                testMemberId,
                "Character 2",
                "small, clever",
                "intelligent",
                "A clever character",
                "http://example.com/character2.jpg",
                "http://example.com/origin2.jpg"
        );
        characterJpaRepository.save(character2);

        BookJpaEntity book2 = BookJpaEntity.builder()
                .id("book-456")
                .userId(testMemberId)
                .characterId(character2.getId())
                .title("Book 2")
                .author("Author 2")
                .createdAt(LocalDateTime.now())
                .bookColor(2L)
                .storyLength(15)
                .coverImageUrl("http://example.com/cover2.jpg")
                .build();
        bookJpaRepository.save(book2);

        Actor currentUser = new Actor(testMemberId, Role.MEMBER);

        // When
        List<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnailsByUser(currentUser);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(BookThumbnail::bookId)
                .containsExactlyInAnyOrder("book-123", "book-456");
        assertThat(result).extracting(BookThumbnail::title)
                .containsExactlyInAnyOrder("Test Book", "Book 2");
        assertThat(result).extracting(BookThumbnail::author)
                .containsExactlyInAnyOrder("Test Author", "Author 2");
    }

    @Test
    @DisplayName("retrieveThumbnailsByUser - should return empty list when user has no books")
    void retrieveThumbnailsByUser_emptyList() {
        // Given - create a new user with no books
        MemberJpaEntity newMember = createMember("newuser", "newpass123");
        memberJpaRepository.save(newMember);
        Actor newUser = new Actor(newMember.getId(), Role.MEMBER);

        // When
        List<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnailsByUser(newUser);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("retrieveById - should handle book with multiple pages correctly")
    void retrieveById_withMultiplePages() {
        // Given - add a third page
        pageJpaRepository.save(createPage(testBookId, "Page 3 content", "http://example.com/page3.jpg", 3));

        // When
        Book result = bookRepositoryAdapter.retrieveById(testBookId);

        // Then
        assertThat(result.bookPages()).hasSize(3);
        assertThat(result.bookPages()).extracting("context")
                .containsExactly("Page 1 content", "Page 2 content", "Page 3 content");
    }

    @Test
    @DisplayName("retrieveThumbnailsByUser - should only return books for the specific user")
    void retrieveThumbnailsByUser_onlyUserBooks() {
        // Given - create another user with their own book
        MemberJpaEntity otherMember = createMember("otheruser", "otherpass123");
        memberJpaRepository.save(otherMember);

        CharacterJpaEntity otherCharacter = createCharacter(
                otherMember.getId(),
                "Other Character",
                "big, strong",
                "brave",
                "A strong character",
                "http://example.com/other-character.jpg",
                "http://example.com/other-origin.jpg"
        );
        characterJpaRepository.save(otherCharacter);

        BookJpaEntity otherBook = BookJpaEntity.builder()
                .id("other-book")
                .userId(otherMember.getId())
                .characterId(otherCharacter.getId())
                .title("Other Book")
                .author("Other Author")
                .createdAt(LocalDateTime.now())
                .bookColor(3L)
                .storyLength(20)
                .coverImageUrl("http://example.com/other-cover.jpg")
                .build();
        bookJpaRepository.save(otherBook);

        Actor currentUser = new Actor(testMemberId, Role.MEMBER);

        // When
        List<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnailsByUser(currentUser);

        // Then - should only return the first user's book, not the other user's book
        assertThat(result).hasSize(1);
        assertThat(result.get(0).bookId()).isEqualTo("book-123");
        assertThat(result.get(0).title()).isEqualTo("Test Book");
    }

    private MemberJpaEntity createMember(String username, String password) {
        return MemberJpaEntity.builder()
                .username(username)
                .password(password)
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
    }

    private CharacterJpaEntity createCharacter(Long memberId, String name, String appearanceKeywords,
                                                String personality, String description,
                                                String imageUrl, String originImageUrl) {
        return CharacterJpaEntity.builder()
                .memberId(memberId)
                .name(name)
                .appearanceKeywords(appearanceKeywords)
                .personality(personality)
                .userDescription(description)
                .imageUrl(imageUrl)
                .originImageUrl(originImageUrl)
                .build();
    }

    private PageJpaEntity createPage(String bookId, String content, String imageUrl, int pageNumber) {
        return PageJpaEntity.builder()
                .bookId(bookId)
                .context(content)
                .imageUrl(imageUrl)
                .pageNumber(pageNumber)
                .build();
    }
}