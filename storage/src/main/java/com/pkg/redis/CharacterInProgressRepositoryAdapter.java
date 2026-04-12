package com.pkg.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkg.domain.character.CharacterInProgress;
import com.pkg.domain.character.CharacterInProgressException;
import com.pkg.domain.character.CharacterInProgressRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CharacterInProgressRepositoryAdapter implements CharacterInProgressRepository {

    private static final String KEY_PREFIX = "char:progress:";
    private static final long TTL_SEC = 1800L;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public CharacterInProgressRepositoryAdapter(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(CharacterInProgress cip) {
        try {
            String json = objectMapper.writeValueAsString(new CipRedisDto(cip));
            redisTemplate.opsForValue().set(KEY_PREFIX + cip.id(), json, TTL_SEC, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("CIP 직렬화 실패: " + cip.id(), e);
        }
    }

    @Override
    public CharacterInProgress getById(String cipId) {
        String raw = redisTemplate.opsForValue().get(KEY_PREFIX + cipId);
        if (raw == null) {
            throw CharacterInProgressException.notFound(cipId);
        }
        try {
            CipRedisDto dto = objectMapper.readValue(raw, CipRedisDto.class);
            return dto.toDomain();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("CIP 역직렬화 실패: " + cipId, e);
        }
    }

    @Override
    public void markAsCompleted(String cipId) {
        String raw = redisTemplate.opsForValue().get(KEY_PREFIX + cipId);
        if (raw == null) {
            return;
        }
        try {
            CipRedisDto dto = objectMapper.readValue(raw, CipRedisDto.class);
            CipRedisDto completed = new CipRedisDto(
                    dto.id(), dto.userId(), dto.name(),
                    dto.appearanceKeywords(), dto.personality(), dto.description(),
                    CharacterInProgress.Status.COMPLETED.name()
            );
            String json = objectMapper.writeValueAsString(completed);
            redisTemplate.opsForValue().set(KEY_PREFIX + cipId, json, 300L, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("CIP 완료 처리 실패: " + cipId, e);
        }
    }

    record CipRedisDto(
            String id,
            Long userId,
            String name,
            String appearanceKeywords,
            String personality,
            String description,
            String status
    ) {
        CipRedisDto(CharacterInProgress cip) {
            this(cip.id(), cip.userId(), cip.name(),
                    cip.appearanceKeywords(), cip.personality(), cip.description(),
                    cip.status().name());
        }

        CharacterInProgress toDomain() {
            return new CharacterInProgress(
                    id, userId, name, appearanceKeywords, personality, description,
                    CharacterInProgress.Status.valueOf(status)
            );
        }
    }
}
