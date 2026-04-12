package com.pkg.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.domain.bookprogress.BookPageResult;
import com.pkg.domain.bookprogress.BookPageResultRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class BookPageResultRepositoryAdapter implements BookPageResultRepository {

    private static final String RESULT_KEY_PREFIX = "bip:result:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public BookPageResultRepositoryAdapter(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<BookPageResult> find(String bipId) {
        String raw = redisTemplate.opsForValue().get(RESULT_KEY_PREFIX + bipId);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(raw, new TypeReference<>() {});
            BookPageResult result = new BookPageResult(
                    (int) map.get("pageIndex"),
                    (String) map.get("context"),
                    (String) map.get("imageUrl"),
                    (List<String>) map.get("questions")
            );
            return Optional.of(result);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String bipId) {
        redisTemplate.delete(RESULT_KEY_PREFIX + bipId);
    }
}
