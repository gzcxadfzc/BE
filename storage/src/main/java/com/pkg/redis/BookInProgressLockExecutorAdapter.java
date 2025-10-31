package com.pkg.redis;

import com.pkg.domain.book.Book;
import com.pkg.domain.bookprogress.AiGenerateResult;
import com.pkg.domain.bookprogress.BookInProgressLockExecutor;
import com.pkg.domain.bookprogress.BookProgressException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class BookInProgressLockExecutorAdapter implements BookInProgressLockExecutor {

    private static final String BIP_LOCK_KEY_PREFIX = "bip:lock:";
    private static final Long EXPIRATION_SEC = 100000L;

    private final RedisLockManager lockManager;

    public BookInProgressLockExecutorAdapter(RedisLockManager lockManager) {
        this.lockManager = lockManager;
    }

    @Override
    public AiGenerateResult updateWithLock(String bipId, Supplier<AiGenerateResult> generator) {
        try {
            String key = BIP_LOCK_KEY_PREFIX + bipId;
            return lockManager.execute(key, EXPIRATION_SEC, generator);
        } catch (RedisLockException e) {
            throw BookProgressException.bookPageAlreadyGenerating(bipId);
        }
    }

    @Override
    public Book saveWithLock(String bipId, Supplier<Book> generator) {
        try {
            String key = BIP_LOCK_KEY_PREFIX + bipId;
            return lockManager.execute(key, EXPIRATION_SEC, generator);
        } catch (RedisLockException e) {
            throw BookProgressException.bookPageAlreadySaving(bipId);
        }
    }
}
