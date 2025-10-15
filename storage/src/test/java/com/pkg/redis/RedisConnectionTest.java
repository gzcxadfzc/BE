package com.pkg.redis;

import com.pkg.config.RedisConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataRedisTest
@Import(RedisConfig.class)
class RedisConnectionTest {

    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    @Autowired
    RedisTemplate<String, BookInProgressRedisEntity> bookInProgressRedisTemplate;

    @Autowired
    RedisTemplate<String, BookPageRedisEntity> bookPageRedisTemplate;

    @Test
    @DisplayName("Redis 연결 확인")
    void testRedisConnection() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            assertThat(connection).isNotNull();
            String ping = connection.ping();
            assertThat(ping).isEqualTo("PONG");
            System.out.println("Redis connection successful: " + ping);
        }
    }

    @Test
    @DisplayName("BookInProgressRedisTemplate 기본 동작 확인")
    void testBookInProgressRedisTemplate() {
        assertThat(bookInProgressRedisTemplate).isNotNull();
        assertThat(bookInProgressRedisTemplate.getConnectionFactory()).isNotNull();

        System.out.println("BookInProgressRedisTemplate bean configured successfully");
    }

    @Test
    @DisplayName("BookPageRedisTemplate 기본 동작 확인")
    void testBookPageRedisTemplate() {
        assertThat(bookPageRedisTemplate).isNotNull();
        assertThat(bookPageRedisTemplate.getConnectionFactory()).isNotNull();

        System.out.println("BookPageRedisTemplate bean configured successfully");
    }

    @Test
    @DisplayName("RedisConnectionFactory 설정 확인")
    void testRedisConnectionFactory() {
        assertThat(redisConnectionFactory).isNotNull();

        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            assertThat(connection.serverCommands().info()).isNotNull();
            System.out.println("RedisConnectionFactory configured successfully");
        }
    }
}
