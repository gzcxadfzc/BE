package com.pkg.redis;

import com.pkg.domain.book.BookPage;
import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.bookprogress.BookInProgressRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookInProgressRepositoryAdapter implements BookInProgressRepository {

    private final BookInProgressRedisRepository bookInProgressRepository;
    private final BookPageRedisRepository pageRedisRepository;

    public BookInProgressRepositoryAdapter(BookInProgressRedisRepository repository, BookPageRedisRepository pageRedisRepository) {
        this.bookInProgressRepository = repository;
        this.pageRedisRepository = pageRedisRepository;
    }

    @Override
    public List<BookInProgress> retrieveByMemberId(Long memberId) {
        List<BookInProgressRedisEntity> bips = bookInProgressRepository.findByMemberId(memberId);
        return bips.stream()
                .map(bip -> {
                    List<BookPageRedisEntity> pages = pageRedisRepository.retrieveAll(bip.id());
                    return new BookInProgress(
                            bip.id(),
                            bip.memberId(),
                            bip.backgroundInfo(),
                            bip.toBookCharacter(),
                            pages.stream()
                                    .map(BookPageRedisEntity::toBookPage)
                                    .toList(),
                            BookInProgressRedisEntity.Status.toDomain(bip.status())
                    );
                }).toList();
    }

    @Override
    public BookInProgress retrieveById(String id) {
        BookInProgressRedisEntity bip = bookInProgressRepository.get(id);
        if (bip == null) {
            return null;
        }

        List<BookPageRedisEntity> pages = pageRedisRepository.retrieveAll(id);

        return new BookInProgress(
                bip.id(),
                bip.memberId(),
                bip.backgroundInfo(),
                bip.toBookCharacter(),
                pages.stream()
                        .map(BookPageRedisEntity::toBookPage)
                        .toList(),
                BookInProgressRedisEntity.Status.toDomain(bip.status())
        );
    }

    @Override
    public BookInProgress save(BookInProgress bookInProgress) {
        // Convert BookCharacter to BookCharacterRedis
        BookInProgressRedisEntity.BookCharacterRedis characterRedis =
                new BookInProgressRedisEntity.BookCharacterRedis(
                        bookInProgress.character().id(),
                        bookInProgress.character().name(),
                        bookInProgress.character().description(),
                        bookInProgress.character().appearanceKeywords(),
                        bookInProgress.character().personality(),
                        bookInProgress.character().imageUrl()
                );

        // Convert BookInProgress to BookInProgressRedisEntity
        BookInProgressRedisEntity entity = new BookInProgressRedisEntity(
                bookInProgress.id(),
                bookInProgress.ownerId(),
                bookInProgress.backgroundInfo(),
                characterRedis,
                bookInProgress.previousPages().size(),
                BookInProgressRedisEntity.Status.fromDomain(bookInProgress.status())
        );

        // Save the book in progress entity
        bookInProgressRepository.put(entity);

        // Delete old pages and save new pages
        pageRedisRepository.deleteAll(bookInProgress.id());
        bookInProgress.previousPages().forEach(page -> {
            BookPageRedisEntity pageEntity = new BookPageRedisEntity(
                    bookInProgress.id(),
                    page.context(),
                    page.imageUrl(),
                    page.pageNumber()
            );
            pageRedisRepository.append(bookInProgress.id(), pageEntity);
        });

        return bookInProgress;
    }

    @Override
    public BookInProgress addPageTo(String id, BookPage bookPage) {
        if(!bookInProgressRepository.has(id)) {
            return null;
        }
        BookPageRedisEntity entity = new BookPageRedisEntity(
                id,
                bookPage.context(),
                bookPage.imageUrl(),
                bookPage.pageNumber()
        );
        pageRedisRepository.append(id, entity);
        List<BookPageRedisEntity> updated = pageRedisRepository.retrieveAll(id);
        BookInProgressRedisEntity bip = bookInProgressRepository.get(id);
        return new BookInProgress(
                bip.id(),
                bip.memberId(),
                bip.backgroundInfo(),
                bip.toBookCharacter(),
                updated.stream()
                        .map(BookPageRedisEntity::toBookPage)
                        .toList(),
                BookInProgressRedisEntity.Status.toDomain(bip.status())
        );
    }
}
