package com.pkg.jpa;

import com.pkg.domain.book.Book;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.book.BookRepository;
import com.pkg.domain.book.BookThumbnail;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.member.Actor;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookRepositoryAdapter implements BookRepository {

    private final BookJpaRepository bookJpaRepository;
    private final PageJpaRepository pageJpaRepository;

    public BookRepositoryAdapter(BookJpaRepository bookJpaRepository, PageJpaRepository pageJpaRepository) {
        this.bookJpaRepository = bookJpaRepository;
        this.pageJpaRepository = pageJpaRepository;
    }

    @Override
    @Transactional
    public Book save(Book book) {
        List<BookPage> pages = book.bookPages();
        BookJpaEntity bookEntity = bookJpaRepository.save(
                BookJpaEntity.builder()
                        .author(book.author())
                        .title(book.title())
                        .id(book.id())
                        .coverImageUrl(pages.get(0).imageUrl())
                        .characterId(book.character().id())
                        .createdAt(LocalDateTime.now())
                        .userId(book.memberId())
                        .storyLength(book.bookPages().size())
                        .bookColor(1L)
                        .build()
        );
        List<PageJpaEntity> pageEntities = pageJpaRepository.saveAll(
            pages.stream()
                .map(page -> PageJpaEntity.builder()
                            .bookId(book.id())
                            .context(page.context())
                            .imageUrl(page.imageUrl())
                            .pageNumber(page.pageNumber())
                            .build())
                .toList()
        );
        return retrieveById(book.id());
    }

    @Override
    public Book retrieveById(String bookId) {
        BookCharacterProjection bookCharacterProjection = bookJpaRepository.retrieveBookId(bookId);
        if(bookCharacterProjection == null) {
            return null;
        }
        List<PageJpaEntity> pages = pageJpaRepository.findAllByBookId(bookId);
        return Book.builder()
                .id(bookCharacterProjection.bookId())
                .title(bookCharacterProjection.title())
                .memberId(bookCharacterProjection.userId())
                .author(bookCharacterProjection.author())
                .character(
                        new BookCharacter(
                                bookCharacterProjection.characterId(),
                                bookCharacterProjection.userId(),
                                bookCharacterProjection.characterName(),
                                bookCharacterProjection.characterAppearanceKeyword(),
                                bookCharacterProjection.characterPersonality(),
                                bookCharacterProjection.characterDescription(),
                                bookCharacterProjection.characterImageUrl()
                        )
                )
                .bookPages(pages.stream().map(PageJpaEntity::toBookPage).toList())
                .build();
    }


    @Override
    public List<BookThumbnail> retrieveThumbnailsByUser(Actor currentUser) {
        return bookJpaRepository.findAllByUserId(currentUser.id())
                .stream()
                .map(BookJpaEntity::toBookThumbnail)
                .toList();
    }
}
