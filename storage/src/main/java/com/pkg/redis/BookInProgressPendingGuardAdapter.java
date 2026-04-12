package com.pkg.redis;

import com.pkg.domain.bookprogress.BookInProgressPendingGuard;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class BookInProgressPendingGuardAdapter implements BookInProgressPendingGuard {

    private static final String GUARD_KEY_PREFIX = "bip:pending-guard:";
    private static final long GUARD_TTL_SEC = 600L;

    private final RedisTemplate<String, String> redisTemplate;

    public BookInProgressPendingGuardAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void set(String bipId) {
        redisTemplate.opsForValue().set(GUARD_KEY_PREFIX + bipId, "1", GUARD_TTL_SEC, TimeUnit.SECONDS);
    }

    @Override
    public void delete(String bipId) {
        redisTemplate.delete(GUARD_KEY_PREFIX + bipId);
    }

    @Override
    public boolean exists(String bipId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(GUARD_KEY_PREFIX + bipId));
    }
}
