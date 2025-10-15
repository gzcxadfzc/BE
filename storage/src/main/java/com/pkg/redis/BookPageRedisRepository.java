package com.pkg.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class BookPageRedisRepository {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(3600L);
    private static final String KEY_PREFIX = "book:pages:";
    private final RedisTemplate<String, BookPageRedisEntity> template;

    public BookPageRedisRepository(RedisTemplate<String, BookPageRedisEntity> template) {
        this.template = template;
    }

    private String key(String bookId) {
        return KEY_PREFIX + bookId;
    }

    public void append(String bookId, BookPageRedisEntity page) {
        Long len = template.opsForList().rightPush(key(bookId), page);
        if (len != null) template.expire(key(bookId), DEFAULT_TTL);
    }

    public List<BookPageRedisEntity> retrieveAll(String bookId) {
        Long size = template.opsForList().size(key(bookId));
        if (size == null || size == 0) return List.of();
        return template.opsForList().range(key(bookId), 0, size - 1);
    }

    public void deleteAll(String bookId) {
        template.delete(key(bookId));
    }
}
