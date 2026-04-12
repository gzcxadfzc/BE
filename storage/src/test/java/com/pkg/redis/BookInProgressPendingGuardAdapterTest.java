package com.pkg.redis;

import com.pkg.config.RedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataRedisTest
@Import({RedisConfig.class, BookInProgressPendingGuardAdapter.class})
@DisplayName("BookInProgressPendingGuardAdapter 테스트")
class BookInProgressPendingGuardAdapterTest {

    @Autowired
    private BookInProgressPendingGuardAdapter adapter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    @AfterEach
    void flushRedis() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    @DisplayName("set 후 exists는 true 반환")
    void set_thenExists_returnsTrue() {
        adapter.set("bip-001");
        assertThat(adapter.exists("bip-001")).isTrue();
    }

    @Test
    @DisplayName("set 하지 않으면 exists는 false 반환")
    void exists_returnsFalse_whenNotSet() {
        assertThat(adapter.exists("bip-none")).isFalse();
    }

    @Test
    @DisplayName("delete 후 exists는 false 반환")
    void delete_thenExists_returnsFalse() {
        adapter.set("bip-002");
        adapter.delete("bip-002");
        assertThat(adapter.exists("bip-002")).isFalse();
    }

    @Test
    @DisplayName("서로 다른 bipId는 독립적")
    void guardKeys_areIsolatedByBipId() {
        adapter.set("bip-A");
        assertThat(adapter.exists("bip-A")).isTrue();
        assertThat(adapter.exists("bip-B")).isFalse();
    }
}
