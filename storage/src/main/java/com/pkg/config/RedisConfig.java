package com.pkg.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pkg.redis.BookInProgressRedisEntity;
import com.pkg.redis.BookPageRedisEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") String port
    ) {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, Integer.parseInt(port)));
    }


    @Bean
    public RedisTemplate<String, BookInProgressRedisEntity> bookInProgressRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, BookInProgressRedisEntity> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        var stringSer = RedisSerializer.string();
        var jsonSer = new Jackson2JsonRedisSerializer<>(BookInProgressRedisEntity.class);
        template.setKeySerializer(stringSer);
        template.setValueSerializer(jsonSer);
        template.setHashKeySerializer(stringSer);
        template.setHashValueSerializer(jsonSer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, BookPageRedisEntity> bookPageRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, BookPageRedisEntity> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        var stringSer = RedisSerializer.string();
        var jsonSer   = new Jackson2JsonRedisSerializer<>(BookPageRedisEntity.class);
        template.setKeySerializer(stringSer);
        template.setValueSerializer(jsonSer);
        template.setHashKeySerializer(stringSer);
        template.setHashValueSerializer(jsonSer);
        template.afterPropertiesSet();
        return template;
    }
}
