package com.pkg.jpa;

import com.pkg.domain.book.*;
import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.character.BookCharacterRepository;
import com.pkg.domain.common.PageInfo;
import com.pkg.domain.common.PageResult;
import com.pkg.domain.member.Actor;
import com.pkg.redis.BookCompleteEvent;
import com.pkg.s3.ImageUploadEvent;
import com.pkg.s3.PreAssignedUrl;
import com.pkg.s3.S3KeyGen;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@Transactional
public class BookRepositoryAdapter implements BookRepository {

    private final BookJpaRepository bookJpaRepository;
    private final BookPageJpaRepository pageJpaRepository;
    private final BookCharacterRepository bookCharacterRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookRepositoryAdapter(
            BookJpaRepository bookJpaRepository,
            BookPageJpaRepository pageJpaRepository,
            ApplicationEventPublisher eventPublisher,
            BookCharacterRepository bookCharacterRepository) {
        this.bookJpaRepository = bookJpaRepository;
        this.pageJpaRepository = pageJpaRepository;
        this.bookCharacterRepository = bookCharacterRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public Book saveFrom(BookInProgress bookInProgress, Function<BookInProgress, Book> converter) {
        Map<String, PreAssignedUrl> urls = PreAssignedUrl.mapOfBookPrefix(
                bookInProgress.previousPages().stream().map(BookPage::imageUrl).toList());
        BookInProgress updated = bookInProgress.changeBookPage(page -> {
            String preAssigned = S3KeyGen.getBucketHost() + urls.get(page.imageUrl()).destinationKey();
            return page.changeImageUrl(preAssigned);
        });
        eventPublisher.publishEvent(new ImageUploadEvent(urls.values().stream().toList()));
        eventPublisher.publishEvent(new BookCompleteEvent(bookInProgress.id()));
        Book book = converter.apply(updated);
        BookJpaEntity bookEntity = bookJpaRepository.save(BookJpaEntity.fromBook(book));
        List<BookPageJpaEntity> pageEntities = pageJpaRepository.saveAll(
                book.bookPages().stream()
                        .map(page -> BookPageJpaEntity.fromBookPage(bookEntity.getId(), page))
                        .toList()
        );
        return new Book(
                bookEntity.getId(),
                bookEntity.getUserId(),
                pageEntities.stream().map(BookPageJpaEntity::toBookPage).toList(),
                bookEntity.getTitle(),
                bookEntity.getAuthor(),
                bookCharacterRepository.retrieveById(bookEntity.getCharacterId())
        );
    }

    @Override
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
