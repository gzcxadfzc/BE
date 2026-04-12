package com.pkg.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.domain.character.CharacterResult;
import com.pkg.domain.character.CharacterResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CharacterResultRepositoryAdapter implements CharacterResultRepository {

    private static final Logger log = LoggerFactory.getLogger(CharacterResultRepositoryAdapter.class);
    private static final String KEY_PREFIX = "char:result:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public CharacterResultRepositoryAdapter(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<CharacterResult> find(String cipId) {
        String raw = redisTemplate.opsForValue().get(KEY_PREFIX + cipId);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, CharacterResult.class));
        } catch (JsonProcessingException e) {
            log.warn("[CharacterResult] JSON 파싱 실패 - cipId: {}", cipId, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String cipId) {
        redisTemplate.delete(KEY_PREFIX + cipId);
    }
}
