package com.pkg.redis;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class BookInProgressRedisRepository {

    private static final String KEY = "book:inprogress:";

    private static final Long EXPIRATION_SEC = 3600L;
    private final RedisTemplate<String, BookInProgressRedisEntity> redisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;

    @Autowired
    public BookInProgressRedisRepository(
            RedisTemplate<String, BookInProgressRedisEntity> bookInProgressRedisTemplate,
            RedisTemplate<String, String> stringRedisTemplate ) {
        this.redisTemplate = bookInProgressRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void put(BookInProgressRedisEntity bookInProgressRedisEntity) {
        String indexKey = "member:" + bookInProgressRedisEntity.memberId() + ":bip";
        stringRedisTemplate.opsForSet().add(indexKey, bookInProgressRedisEntity.id());
        redisTemplate.opsForValue().set(KEY + bookInProgressRedisEntity.id(), bookInProgressRedisEntity);
        redisTemplate.expire(KEY + bookInProgressRedisEntity.id(), EXPIRATION_SEC, TimeUnit.SECONDS);
    }


    public BookInProgressRedisEntity get(String id) {
        return redisTemplate.opsForValue().get(KEY + id);
    }

    public List<BookInProgressRedisEntity> findByMemberId(String memberId) {
        String indexKey = "member:" + memberId + ":bip";
        Set<String> ids = stringRedisTemplate.opsForSet().members(indexKey);
        if (ids == null) return List.of();
        return ids.stream()
                .map(id -> {
                    BookInProgressRedisEntity bip = this.get(id);
                    if(bip == null) {
                        stringRedisTemplate.opsForSet().remove(indexKey, id);
                    }
                    return bip;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public Boolean delete(String id) {
        return redisTemplate.delete(KEY + id);
    }

    public boolean has(String id) {
       return redisTemplate.hasKey(KEY + id);
    }
}
