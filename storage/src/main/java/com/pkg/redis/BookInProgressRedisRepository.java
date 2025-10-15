package com.pkg.redis;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class BookInProgressRedisRepository {

    private static final String KEY = "book:inprogress:";
    private static final Long EXPIRATION_SEC = 3600L;
    private final HashOperations<String, String, BookInProgressRedisEntity> hashOperations;

    @Autowired
    public BookInProgressRedisRepository(RedisTemplate<String, BookInProgressRedisEntity> bookInProgressRedisTemplate) {
        this.hashOperations  = bookInProgressRedisTemplate.opsForHash();
        bookInProgressRedisTemplate.expire(KEY, EXPIRATION_SEC, TimeUnit.SECONDS);
    }

    public void put(BookInProgressRedisEntity bookInProgressRedis) {
        hashOperations.put(KEY, bookInProgressRedis.memberId(), bookInProgressRedis);
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

    public boolean existsByUserId(Long userId) {
       return hashOperations.hasKey(KEY, String.valueOf(userId));
    }
}
