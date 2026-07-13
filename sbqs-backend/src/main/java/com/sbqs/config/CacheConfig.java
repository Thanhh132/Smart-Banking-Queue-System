package com.sbqs.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);
    private static final long CACHE_WARNING_INTERVAL_MS = Duration.ofMinutes(1).toMillis();
    private final ConcurrentHashMap<String, Long> lastCacheWarnings = new ConcurrentHashMap<>();

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(
            @Value("${sbqs.cache.default-ttl-minutes:5}") long ttlMinutes,
            ObjectMapper objectMapper) {

        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.sbqs.")
                        .allowIfSubType("java.util.")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(ttlMinutes))
                .disableCachingNullValues()
                .prefixCacheNameWith("sbqs:")
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RedisCacheConfiguration defaultConfiguration) {
        return builder -> builder
                .withCacheConfiguration(
                        "queueMonitor",
                        defaultConfiguration.entryTtl(Duration.ofMinutes(1)))
                .withCacheConfiguration(
                        "branches",
                        defaultConfiguration.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(
                        "services",
                        defaultConfiguration.entryTtl(Duration.ofMinutes(5)));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                if (shouldLogCacheWarning("get", cache)) {
                    log.warn("Redis cache get failed cache={} key={} cause={}", cache.getName(), key, exception.getMessage());
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                if (shouldLogCacheWarning("put", cache)) {
                    log.warn("Redis cache put failed cache={} key={} cause={}", cache.getName(), key, exception.getMessage());
                }
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                if (shouldLogCacheWarning("evict", cache)) {
                    log.warn("Redis cache evict failed cache={} key={} cause={}", cache.getName(), key, exception.getMessage());
                }
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                if (shouldLogCacheWarning("clear", cache)) {
                    log.warn("Redis cache clear failed cache={} cause={}", cache.getName(), exception.getMessage());
                }
            }
        };
    }

    /** Khi Redis tắt, mỗi loại lỗi/cache chỉ ghi log tối đa một lần mỗi phút. */
    private boolean shouldLogCacheWarning(String operation, Cache cache) {
        long now = System.currentTimeMillis();
        String warningKey = operation + ":" + cache.getName();
        return lastCacheWarnings.compute(warningKey, (key, lastLoggedAt) ->
                lastLoggedAt == null || now - lastLoggedAt >= CACHE_WARNING_INTERVAL_MS
                        ? now
                        : lastLoggedAt) == now;
    }
}
