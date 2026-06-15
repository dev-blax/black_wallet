package com.james.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

/**
 * Caching configuration (Phase 7).
 *
 * @EnableCaching turns on Spring's cache abstraction: methods annotated with @Cacheable
 * have their results stored, and @CacheEvict removes entries. With spring-boot-starter-data-redis
 * on the classpath and spring.cache.type=redis, Spring Boot auto-configures a RedisCacheManager;
 * this class only customizes it.
 *
 * Cache name "accountViews": holds AccountResponse DTOs keyed by account id. We store a DTO,
 * NOT the JPA Account entity, because the entity has a LAZY user association that cannot be
 * serialized to Redis. Values are serialized as JSON (readable in redis-cli), with a 10-minute
 * TTL as a safety net so a missed eviction can't serve a stale balance forever.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ACCOUNT_VIEWS = "accountViews";

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheManagerCustomizer(ObjectMapper objectMapper) {
        // Reuse Spring Boot's ObjectMapper so Java records (AccountResponse) deserialize correctly.
        var serializer = new Jackson2JsonRedisSerializer<>(objectMapper, AccountResponse.class);

        RedisCacheConfiguration accountViews = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(serializer));

        return builder -> builder.withCacheConfiguration(ACCOUNT_VIEWS, accountViews);
    }
}
