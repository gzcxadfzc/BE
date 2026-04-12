package com.pkg.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.config.RedisConfig;
import com.pkg.domain.bookprogress.BookPageResult;
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataRedisTest
@Import({RedisConfig.class, BookPageResultRepositoryAdapter.class})
@DisplayName("BookPageResultRepositoryAdapter 테스트")
class BookPageResultRepositoryAdapterTest {

    @Autowired
    private BookPageResultRepositoryAdapter adapter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @AfterEach
    void flushRedis() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    @DisplayName("결과가 없으면 Optional.empty() 반환")
    void find_shouldReturnEmpty_whenNoResult() {
        Optional<BookPageResult> result = adapter.find("non-existent");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Lambda가 저장한 JSON 결과를 정상적으로 읽음")
    void find_shouldReturnResult_whenResultExists() throws Exception {
        String bipId = "bip-001";
        String json = """
                {
                  "pageIndex": 0,
                  "context": "Alice found a magical door",
                  "imageUrl": "https://s3.example.com/bip/bip-001/page-0.png",
                  "questions": ["What did Alice see?", "Where did she go?"]
                }
                """;
        redisTemplate.opsForValue().set("bip:result:" + bipId, json, 600, TimeUnit.SECONDS);

        Optional<BookPageResult> result = adapter.find(bipId);

        assertThat(result).isPresent();
        assertThat(result.get().pageIndex()).isEqualTo(0);
        assertThat(result.get().context()).isEqualTo("Alice found a magical door");
        assertThat(result.get().imageUrl()).isEqualTo("https://s3.example.com/bip/bip-001/page-0.png");
        assertThat(result.get().questions()).containsExactly("What did Alice see?", "Where did she go?");
    }

    @Test
    @DisplayName("delete 후 find는 empty 반환")
    void delete_shouldRemoveResult() throws Exception {
        String bipId = "bip-002";
        String json = """
                {"pageIndex":1,"context":"story","imageUrl":"https://s3.example.com/img.png","questions":[]}
                """;
        redisTemplate.opsForValue().set("bip:result:" + bipId, json, 600, TimeUnit.SECONDS);

        adapter.delete(bipId);

        assertThat(adapter.find(bipId)).isEmpty();
    }

    @Test
    @DisplayName("잘못된 JSON이면 Optional.empty() 반환")
    void find_shouldReturnEmpty_whenMalformedJson() {
        String bipId = "bip-bad";
        redisTemplate.opsForValue().set("bip:result:" + bipId, "not-valid-json", 600, TimeUnit.SECONDS);

        Optional<BookPageResult> result = adapter.find(bipId);

        assertThat(result).isEmpty();
    }
}
