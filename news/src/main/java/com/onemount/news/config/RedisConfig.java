package com.onemount.news.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.onemount.news.dto.NewsResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Jackson2JsonRedisSerializer<NewsResponse> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, NewsResponse.class);

        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .build();

        return new LoggingCacheManager(redisCacheManager);
    }

    private static class LoggingCacheManager implements CacheManager {

        private final CacheManager delegate;
        private final Map<String, Cache> decoratedCaches = new ConcurrentHashMap<>();

        LoggingCacheManager(CacheManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public Cache getCache(String name) {
            Cache cache = delegate.getCache(name);
            if (cache == null) {
                return null;
            }
            return decoratedCaches.computeIfAbsent(name, k -> new LoggingCacheDecorator(cache));
        }

        @Override
        public Collection<String> getCacheNames() {
            return delegate.getCacheNames();
        }
    }
}
