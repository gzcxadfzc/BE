package com.pkg.redis;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class BookInProgressRedisService {
    private static final String KEY = "book_in_progress";
    private static final Long EXPIRATION_SEC = 3600L;
    private final HashOperations<String, String, BookInProgressRedisEntity> hashOperations;

    @Autowired
    public BookInProgressRedisService(RedisTemplate<String, BookInProgressRedisEntity> bookInProgressRedisRedisTemplate) {
        this.hashOperations = bookInProgressRedisRedisTemplate.opsForHash();
        bookInProgressRedisRedisTemplate.expire(KEY, EXPIRATION_SEC, TimeUnit.SECONDS);
    }

    public void put(BookInProgressRedisEntity bookInProgressRedis) {
        validate(bookInProgressRedis);
        hashOperations.put(KEY, bookInProgressRedis.userId().toString(), bookInProgressRedis);
    }


    public BookInProgressRedisEntity getByUserId(Long userId) {
        String userIdString = String.valueOf(userId);
        return hashOperations.get(KEY, userIdString);
    }

    public BookInProgressRedisEntity deleteByUserId(Long userId) {
        BookInProgressRedisEntity deletedBookInProgressRedis = getByUserId(userId);
        hashOperations.delete(KEY, String.valueOf(userId));
        return deletedBookInProgressRedis;
    }

    public BookInProgressRedisEntity update(BookInProgressRedisEntity bookInProgressRedis) {
        Long userId = bookInProgressRedis.userId();
        if(existsByUserId(userId)) {
            hashOperations.put(KEY, String.valueOf(bookInProgressRedis.userId()), bookInProgressRedis);
            return getByUserId(userId);
        }
        throw new IllegalArgumentException("no redis entity");
    }

    public boolean existsByUserId(Long userId) {
       return hashOperations.hasKey(KEY, String.valueOf(userId));
    }

    private void validate(BookInProgressRedisEntity bookInProgressRedis) {
        if (bookInProgressRedis.bookId() == null || bookInProgressRedis.userId() == null) {
            throw new IllegalArgumentException("bookInProgress field cannot be null");
        }
    }
}
