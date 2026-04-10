package com.pkg.redis;

import com.pkg.config.RedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@DataRedisTest
@Import({RedisConfig.class, RedisLockManager.class})
class RedisLockManagerTest {

    @Autowired
    private RedisLockManager lockManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String KEY = "test:lock:key";
    private static final long EXPIRE_MS = 5000L;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(KEY);
    }

    @AfterEach
    void cleanup() {
        redisTemplate.delete(KEY);
    }

    @Test
    @DisplayName("정상 실행 후 락 해제")
    void execute_releasesLock_afterSuccess() {
        lockManager.execute(KEY, EXPIRE_MS, () -> "result");

        assertThat(redisTemplate.hasKey(KEY)).isFalse();
    }

    @Test
    @DisplayName("작업 중 예외 발생 시 락 해제 (finally 보장)")
    void execute_releasesLock_afterException() {
        assertThatThrownBy(() ->
            lockManager.execute(KEY, EXPIRE_MS, () -> { throw new RuntimeException("fail"); })
        ).isInstanceOf(RuntimeException.class);

        assertThat(redisTemplate.hasKey(KEY)).isFalse();
    }

    @Test
    @DisplayName("락 보유 중 재획득 시도 시 RedisLockException 발생")
    void execute_throwsRedisLockException_whenLockAlreadyHeld() throws InterruptedException {
        // 첫 번째 락 보유 상태 유지를 위해 직접 키 설정
        redisTemplate.opsForValue().set(KEY, "other-owner", EXPIRE_MS, TimeUnit.MILLISECONDS);

        assertThatThrownBy(() -> lockManager.execute(KEY, EXPIRE_MS, () -> "result"))
            .isInstanceOf(RedisLockException.class);
    }

    @Test
    @DisplayName("Lua script atomic 해제 - 다른 소유자 lockId 보유 시 DEL 하지 않음")
    void releaseLock_doesNotDelete_whenLockIdMismatch() {
        // 락 획득 후 작업 실행 직전, 외부에서 키 값을 덮어씀 (TTL 만료 + 재획득 시뮬레이션)
        String anotherOwnerId = "another-owner-id";

        lockManager.execute(KEY, EXPIRE_MS, () -> {
            // 작업 도중 다른 소유자가 락을 가져간 상황 시뮬레이션
            redisTemplate.opsForValue().set(KEY, anotherOwnerId);
            return null;
        });

        // Lua script는 lockId 불일치 → DEL 하지 않음 → 다른 소유자의 락이 살아있어야 함
        String remaining = redisTemplate.opsForValue().get(KEY);
        assertThat(remaining).isEqualTo(anotherOwnerId);
    }

    @Test
    @DisplayName("action 반환값이 그대로 반환됨")
    void execute_returnsActionResult() {
        String result = lockManager.execute(KEY, EXPIRE_MS, () -> "expected");

        assertThat(result).isEqualTo("expected");
    }

    @Test
    @DisplayName("락 해제 후 동일 키로 재획득 가능")
    void execute_canReacquireLock_afterRelease() {
        lockManager.execute(KEY, EXPIRE_MS, () -> "first");
        String result = lockManager.execute(KEY, EXPIRE_MS, () -> "second");

        assertThat(result).isEqualTo("second");
        assertThat(redisTemplate.hasKey(KEY)).isFalse();
    }
}
