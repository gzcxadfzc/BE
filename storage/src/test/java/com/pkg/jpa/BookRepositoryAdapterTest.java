package com.pkg.jpa;

import com.pkg.domain.book.Book;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.book.BookRetrieveQuery;
import com.pkg.domain.book.BookThumbnail;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.common.PageResult;
import com.pkg.domain.common.SliceResult;
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
@Import({BookRepositoryAdapter.class, BookCharacterRepositoryAdapter.class})
class BookRepositoryAdapterTest {

    @Autowired
    private BookRepositoryAdapter bookRepositoryAdapter;

    @Autowired
    private BookJpaRepository bookJpaRepository;

    @Autowired
    private CharacterJpaRepository characterJpaRepository;

    @Autowired
    private BookPageJpaRepository pageJpaRepository;

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
        List<BookPageJpaEntity> savedPages = pageJpaRepository.findAllByBookId(newBookId);
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
        List<BookPageJpaEntity> savedPages = pageJpaRepository.findAllByBookId(bookId);
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

    @Test
    @DisplayName("retrieveThumbnails - should retrieve thumbnails with default pagination sorted by created date desc")
    void retrieveThumbnails_withDefaultPagination() {
        // Given - create additional books to test pagination
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
                .createdAt(LocalDateTime.now().plusDays(1))
                .bookColor(2L)
                .storyLength(15)
                .coverImageUrl("http://example.com/cover2.jpg")
                .build();
        bookJpaRepository.save(book2);

        BookRetrieveQuery query = new BookRetrieveQuery(0, 10, BookRetrieveQuery.SortOption.CREATED_AT_DESC);

        // When
        PageResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnails(query);
        List<BookThumbnail> thumbnails = result.getElements();

        // Then
        assertThat(thumbnails).hasSize(2);
        // Should be sorted by created date descending (newest first)
        assertThat(thumbnails.get(0).bookId()).isEqualTo("book-456");
        assertThat(thumbnails.get(1).bookId()).isEqualTo("book-123");
    }

    @Test
    @DisplayName("retrieveThumbnails - should sort by created date ascending")
    void retrieveThumbnails_sortByCreatedAtAsc() {
        // Given
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
                .createdAt(LocalDateTime.now().plusDays(1))
                .bookColor(2L)
                .storyLength(15)
                .coverImageUrl("http://example.com/cover2.jpg")
                .build();
        bookJpaRepository.save(book2);

        BookRetrieveQuery query = new BookRetrieveQuery(0, 10, BookRetrieveQuery.SortOption.CREATED_AT_ASC);

        // When
        PageResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnails(query);
        List<BookThumbnail> thumbnails = result.getElements();

        // Then
        assertThat(thumbnails).hasSize(2);
        // Should be sorted by created date ascending (oldest first)
        assertThat(thumbnails.get(0).bookId()).isEqualTo("book-123");
        assertThat(thumbnails.get(1).bookId()).isEqualTo("book-456");
    }

    @Test
    @DisplayName("retrieveThumbnails - should sort by title descending")
    void retrieveThumbnails_sortByTitleDesc() {
        // Given
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

        BookJpaEntity bookA = BookJpaEntity.builder()
                .id("book-aaa")
                .userId(testMemberId)
                .characterId(character2.getId())
                .title("Alpha Book")
                .author("Author A")
                .createdAt(LocalDateTime.now())
                .bookColor(2L)
                .storyLength(15)
                .coverImageUrl("http://example.com/cover-a.jpg")
                .build();
        bookJpaRepository.save(bookA);

        BookJpaEntity bookZ = BookJpaEntity.builder()
                .id("book-zzz")
                .userId(testMemberId)
                .characterId(character2.getId())
                .title("Zebra Book")
                .author("Author Z")
                .createdAt(LocalDateTime.now())
                .bookColor(3L)
                .storyLength(20)
                .coverImageUrl("http://example.com/cover-z.jpg")
                .build();
        bookJpaRepository.save(bookZ);

        BookRetrieveQuery query = new BookRetrieveQuery(0, 10, BookRetrieveQuery.SortOption.TITLE_DESC);

        // When
        PageResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnails(query);
        List<BookThumbnail> thumbnails = result.getElements();
        // Then
        assertThat(thumbnails).hasSize(3);
        // Should be sorted by title descending (Z to A)
        assertThat(thumbnails.get(0).title()).isEqualTo("Zebra Book");
        assertThat(thumbnails.get(1).title()).isEqualTo("Test Book");
        assertThat(thumbnails.get(2).title()).isEqualTo("Alpha Book");
    }

    @Test
    @DisplayName("retrieveThumbnails - should sort by title ascending")
    void retrieveThumbnails_sortByTitleAsc() {
        // Given
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

        BookJpaEntity bookA = BookJpaEntity.builder()
                .id("book-aaa")
                .userId(testMemberId)
                .characterId(character2.getId())
                .title("Alpha Book")
                .author("Author A")
                .createdAt(LocalDateTime.now())
                .bookColor(2L)
                .storyLength(15)
                .coverImageUrl("http://example.com/cover-a.jpg")
                .build();
        bookJpaRepository.save(bookA);

        BookJpaEntity bookZ = BookJpaEntity.builder()
                .id("book-zzz")
                .userId(testMemberId)
                .characterId(character2.getId())
                .title("Zebra Book")
                .author("Author Z")
                .createdAt(LocalDateTime.now())
                .bookColor(3L)
                .storyLength(20)
                .coverImageUrl("http://example.com/cover-z.jpg")
                .build();
        bookJpaRepository.save(bookZ);

        BookRetrieveQuery query = new BookRetrieveQuery(0, 10, BookRetrieveQuery.SortOption.TITLE_ASC);

        // When
        PageResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnails(query);
        List<BookThumbnail> thumbnails = result.getElements();
        // Then
        assertThat(thumbnails).hasSize(3);
        // Should be sorted by title ascending (A to Z)
        assertThat(thumbnails.get(0).title()).isEqualTo("Alpha Book");
        assertThat(thumbnails.get(1).title()).isEqualTo("Test Book");
        assertThat(thumbnails.get(2).title()).isEqualTo("Zebra Book");
    }

    @Test
    @DisplayName("retrieveThumbnails - should respect page size limit")
    void retrieveThumbnails_withPageSizeLimit() {
        // Given - create 5 books
        for (int i = 1; i <= 4; i++) {
            CharacterJpaEntity character = createCharacter(
                    testMemberId,
                    "Character " + i,
                    "keywords " + i,
                    "personality " + i,
                    "description " + i,
                    "http://example.com/char" + i + ".jpg",
                    "http://example.com/origin" + i + ".jpg"
            );
            characterJpaRepository.save(character);

            BookJpaEntity book = BookJpaEntity.builder()
                    .id("book-" + i)
                    .userId(testMemberId)
                    .characterId(character.getId())
                    .title("Book " + i)
                    .author("Author " + i)
                    .createdAt(LocalDateTime.now().plusDays(i))
                    .bookColor(Long.valueOf(i))
                    .storyLength(10 + i)
                    .coverImageUrl("http://example.com/cover" + i + ".jpg")
                    .build();
            bookJpaRepository.save(book);
        }

        BookRetrieveQuery query = new BookRetrieveQuery(0, 3, BookRetrieveQuery.SortOption.CREATED_AT_DESC);

        // When
        PageResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnails(query);
        List<BookThumbnail> thumbnails = result.getElements();
        // Then - should only return 3 books
        assertThat(thumbnails).hasSize(3);
    }

    @Test
    @DisplayName("retrieveThumbnails - should handle pagination with page index")
    void retrieveThumbnails_withPagination() {
        // Given - create 6 additional books (total 7 with the one in setUp)
        for (int i = 1; i <= 6; i++) {
            CharacterJpaEntity character = createCharacter(
                    testMemberId,
                    "Character " + i,
                    "keywords " + i,
                    "personality " + i,
                    "description " + i,
                    "http://example.com/char" + i + ".jpg",
                    "http://example.com/origin" + i + ".jpg"
            );
            characterJpaRepository.save(character);

            BookJpaEntity book = BookJpaEntity.builder()
                    .id("book-" + i)
                    .userId(testMemberId)
                    .characterId(character.getId())
                    .title("Book " + i)
                    .author("Author " + i)
                    .createdAt(LocalDateTime.now().plusDays(i))
                    .bookColor(Long.valueOf(i))
                    .storyLength(10 + i)
                    .coverImageUrl("http://example.com/cover" + i + ".jpg")
                    .build();
            bookJpaRepository.save(book);
        }

        // When - get first page (index 0, size 2)
        BookRetrieveQuery queryPage1 = new BookRetrieveQuery(0, 2, BookRetrieveQuery.SortOption.CREATED_AT_DESC);
        PageResult<BookThumbnail> resultPage1 = bookRepositoryAdapter.retrieveThumbnails(queryPage1);

        // When - get last page (index 3, size 2)
        BookRetrieveQuery queryPage3 = new BookRetrieveQuery(3, 2, BookRetrieveQuery.SortOption.CREATED_AT_DESC);
        PageResult<BookThumbnail> resultPage3 = bookRepositoryAdapter.retrieveThumbnails(queryPage3);


        // Then
        assertThat(resultPage1.getElements()).hasSize(2);
        assertThat(resultPage3.getElements()).hasSize(1);
    }

    @Test
    @DisplayName("retrieveThumbnails - should return empty list when page index exceeds available books")
    void retrieveThumbnails_emptyPageBeyondRange() {
        // Given - only 1 book exists in setUp
        BookRetrieveQuery query = new BookRetrieveQuery(10, 10, BookRetrieveQuery.SortOption.CREATED_AT_DESC);

        // When
        PageResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnails(query);
        List<BookThumbnail> thumbnails = result.getElements();

        // Then
        assertThat(thumbnails).isEmpty();
    }

    @Test
    @DisplayName("retrieveThumbnailsSlice - 책 목록을 Slice로 조회할 수 있다")
    void retrieveThumbnailsSlice_basic() {
        // Given
        BookRetrieveQuery query = new BookRetrieveQuery(0, 10, BookRetrieveQuery.SortOption.CREATED_AT_DESC);

        // When
        SliceResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnailsSlice(query);

        // Then
        assertThat(result.getElements()).hasSize(1);
        assertThat(result.getIndex()).isEqualTo(0);
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getElements().get(0).bookId()).isEqualTo("book-123");
    }

    @Test
    @DisplayName("retrieveThumbnailsSlice - 다음 페이지가 있으면 hasNext가 true다")
    void retrieveThumbnailsSlice_hasNextTrue() {
        // Given - 책 3권 추가 (총 4권)
        for (int i = 1; i <= 3; i++) {
            CharacterJpaEntity character = createCharacter(
                    testMemberId, "Character " + i, "keywords", "personality", "desc",
                    "http://example.com/char" + i + ".jpg", "http://example.com/origin" + i + ".jpg"
            );
            characterJpaRepository.save(character);
            bookJpaRepository.save(BookJpaEntity.builder()
                    .id("book-extra-" + i)
                    .userId(testMemberId)
                    .characterId(character.getId())
                    .title("Book " + i)
                    .author("Author " + i)
                    .createdAt(LocalDateTime.now().plusDays(i))
                    .bookColor(1L).storyLength(3)
                    .coverImageUrl("http://example.com/cover" + i + ".jpg")
                    .build());
        }

        BookRetrieveQuery query = new BookRetrieveQuery(0, 2, BookRetrieveQuery.SortOption.CREATED_AT_DESC);

        // When
        SliceResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnailsSlice(query);

        // Then
        assertThat(result.getElements()).hasSize(2);
        assertThat(result.isHasNext()).isTrue();
    }

    @Test
    @DisplayName("retrieveThumbnailsSlice - 마지막 페이지면 hasNext가 false다")
    void retrieveThumbnailsSlice_hasNextFalse() {
        // Given - 책 1권만 존재
        BookRetrieveQuery query = new BookRetrieveQuery(0, 2, BookRetrieveQuery.SortOption.CREATED_AT_DESC);

        // When
        SliceResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnailsSlice(query);

        // Then
        assertThat(result.getElements()).hasSize(1);
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("retrieveThumbnailsSlice - created_at DESC 정렬이 적용된다")
    void retrieveThumbnailsSlice_sortByCreatedAtDesc() {
        // Given - 2권 추가
        CharacterJpaEntity character2 = createCharacter(
                testMemberId, "Character 2", "k", "p", "d",
                "http://example.com/c2.jpg", "http://example.com/o2.jpg"
        );
        characterJpaRepository.save(character2);
        bookJpaRepository.save(BookJpaEntity.builder()
                .id("book-newer")
                .userId(testMemberId)
                .characterId(character2.getId())
                .title("Newer Book")
                .author("Author 2")
                .createdAt(LocalDateTime.now().plusDays(1))
                .bookColor(1L).storyLength(3)
                .coverImageUrl("http://example.com/newer.jpg")
                .build());

        BookRetrieveQuery query = new BookRetrieveQuery(0, 10, BookRetrieveQuery.SortOption.CREATED_AT_DESC);

        // When
        SliceResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnailsSlice(query);

        // Then - 최신 책이 먼저
        assertThat(result.getElements()).hasSize(2);
        assertThat(result.getElements().get(0).bookId()).isEqualTo("book-newer");
        assertThat(result.getElements().get(1).bookId()).isEqualTo("book-123");
    }

    @Test
    @DisplayName("retrieveThumbnailsSlice - 책이 없으면 빈 결과를 반환한다")
    void retrieveThumbnailsSlice_empty() {
        // Given
        pageJpaRepository.deleteAll();
        bookJpaRepository.deleteAll();
        BookRetrieveQuery query = new BookRetrieveQuery(0, 10, BookRetrieveQuery.SortOption.CREATED_AT_DESC);

        // When
        SliceResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnailsSlice(query);

        // Then
        assertThat(result.getElements()).isEmpty();
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("retrieveThumbnailsSlice - 두 번째 페이지를 조회할 수 있다")
    void retrieveThumbnailsSlice_secondPage() {
        // Given - 책 4권 추가 (총 5권)
        for (int i = 1; i <= 4; i++) {
            CharacterJpaEntity character = createCharacter(
                    testMemberId, "Character " + i, "k", "p", "d",
                    "http://example.com/c" + i + ".jpg", "http://example.com/o" + i + ".jpg"
            );
            characterJpaRepository.save(character);
            bookJpaRepository.save(BookJpaEntity.builder()
                    .id("book-p" + i)
                    .userId(testMemberId)
                    .characterId(character.getId())
                    .title("Book " + i)
                    .author("Author " + i)
                    .createdAt(LocalDateTime.now().plusDays(i))
                    .bookColor(1L).storyLength(3)
                    .coverImageUrl("http://example.com/cover" + i + ".jpg")
                    .build());
        }

        // When - 두 번째 페이지 (index=1, size=2)
        BookRetrieveQuery query = new BookRetrieveQuery(1, 2, BookRetrieveQuery.SortOption.CREATED_AT_DESC);
        SliceResult<BookThumbnail> result = bookRepositoryAdapter.retrieveThumbnailsSlice(query);

        // Then
        assertThat(result.getElements()).hasSize(2);
        assertThat(result.getIndex()).isEqualTo(1);
    }

    private MemberJpaEntity createMember(String username, String password) {
        return MemberJpaEntity.builder()
                .username(username)
                .password(password)
                .role(RoleJpa.MEMBER)
                .authProvider("local")
                .build();
    }

    private CharacterJpaEntity createCharacter(
            Long memberId,
            String name,
            String appearanceKeywords,
            String personality,
            String description,
            String imageUrl,
            String originImageUrl
    ) {
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

    private BookPageJpaEntity createPage(String bookId, String content, String imageUrl, int pageNumber) {
        return BookPageJpaEntity.builder()
                .bookId(bookId)
                .context(content)
                .imageUrl(imageUrl)
                .pageNumber(pageNumber)
                .build();
    }
}