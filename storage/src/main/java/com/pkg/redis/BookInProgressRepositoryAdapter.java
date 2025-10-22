package com.pkg.redis;

import com.pkg.domain.bookprogress.BookInProgress;
import com.pkg.domain.bookprogress.BookInProgressRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookInProgressRepositoryAdapter implements BookInProgressRepository {

    private final BookInProgressRedisRepository bookInPorgressRepository;
    private final BookPageRedisRepository pageRedisRepository;

    public BookInProgressRepositoryAdapter(BookInProgressRedisRepository repository, BookPageRedisRepository pageRedisRepository) {
        this.bookInPorgressRepository = repository;
        this.pageRedisRepository = pageRedisRepository;
    }

    @Override
    public List<BookInProgress> retrieveByMemberId(Long memberId) {
        List<BookInProgressRedisEntity> bips = bookInPorgressRepository.findByMemberId(memberId);
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
                                    .toList()
                    );
                }).toList();
    }

    @Override
    public BookInProgress retrieveById(String id) {
        return null;
    }

    @Override
    public void save(BookInProgress bookInProgress) {

    }
}
