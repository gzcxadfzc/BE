package com.pkg.jpa;

import com.pkg.domain.book.*;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.common.PageInfo;
import com.pkg.domain.common.PageResult;
import com.pkg.domain.member.Actor;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookRepositoryAdapter implements BookRepository {

    private final BookJpaRepository bookJpaRepository;
    private final BookPageJpaRepository pageJpaRepository;

    public BookRepositoryAdapter(BookJpaRepository bookJpaRepository, BookPageJpaRepository pageJpaRepository) {
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
        List<BookPageJpaEntity> pageEntities = pageJpaRepository.saveAll(
            pages.stream()
                .map(page -> BookPageJpaEntity.builder()
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
        List<BookPageJpaEntity> pages = pageJpaRepository.findAllByBookId(bookId);
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
                .bookPages(pages.stream().map(BookPageJpaEntity::toBookPage).toList())
                .build();
    }


    @Override
    public List<BookThumbnail> retrieveThumbnailsByUser(Actor currentUser) {
        return bookJpaRepository.findAllByUserId(currentUser.id())
                .stream()
                .map(BookJpaEntity::toBookThumbnail)
                .toList();
    }

    @Override
    public PageResult<BookThumbnail> retrieveThumbnails(BookRetrieveQuery query) {
        Pageable pageable = BookRetrieveQueryMapper.toPageable(query);
        Page<BookJpaEntity> entityPage = bookJpaRepository.findAll(pageable);
        List<BookThumbnail> thumbnails = entityPage.get()
                .map(BookJpaEntity::toBookThumbnail)
                .toList();
        return new PageResult<>(
                thumbnails,
                new PageInfo(
                        entityPage.getNumber(),
                        entityPage.getTotalPages(),
                        entityPage.getTotalElements(),
                        entityPage.isLast()
                ));
    }
}
