package com.pkg.redis;

import com.pkg.domain.uitl.UuidGen;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
public class RedisLockManager
{
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT;

    static {
        RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>();
        RELEASE_LOCK_SCRIPT.setScriptText(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('DEL', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end"
        );
        RELEASE_LOCK_SCRIPT.setResultType(Long.class);
    }

    private final RedisTemplate<String, String> redisTemplate;

    public RedisLockManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public <T> T execute (String key, long expireMillis, Supplier<T> action) {
        String lockId = tryLock(key, expireMillis);
        if (lockId == null) {
            throw new RedisLockException(key + "is generating... ");
        }
        try {
            return action.get();
        } finally {
            releaseLock(key, lockId);
        }
    }

    private String tryLock(String key, long expireMillis) {
        String lockId = UuidGen.compact();
        Boolean success = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            byte[] k = redisTemplate.getStringSerializer().serialize(key);
            byte[] v = redisTemplate.getStringSerializer().serialize(lockId);
            return connection.set(k, v, Expiration.milliseconds(expireMillis),
                    RedisStringCommands.SetOption.SET_IF_ABSENT);
        });
        return Boolean.TRUE.equals(success) ? lockId : null;
    }

    private void releaseLock(String key, String lockId) {
        redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(key), lockId);
    }
}
