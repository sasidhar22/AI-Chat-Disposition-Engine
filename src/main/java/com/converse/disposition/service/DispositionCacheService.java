package com.converse.disposition.service;

import com.converse.disposition.model.DispositionCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DispositionCacheService {

    private final RedisTemplate<String, DispositionCard> redisTemplate;
    private final DispositionMetrics metrics;

    @Value("${disposition.redis.cache-ttl-hours:24}")
    private long cacheTtlHours;

    @Value("${disposition.redis.key-prefix:disposition}")
    private String keyPrefix;

    public DispositionCacheService(RedisTemplate<String, DispositionCard> redisTemplate,
                                   DispositionMetrics metrics) {
        this.redisTemplate = redisTemplate;
        this.metrics = metrics;
    }

    public void put(DispositionCard card) {
        String key = buildKey(card.tenantId(), Long.parseLong(card.sessionId()));
        try {
            redisTemplate.opsForValue().set(key, card, cacheTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to cache disposition for key={}", key, e);
        }
    }

    public Optional<DispositionCard> get(String tenantId, long sessionId) {
        String key = buildKey(tenantId, sessionId);
        try {
            DispositionCard card = redisTemplate.opsForValue().get(key);
            if (card != null) {
                metrics.recordCacheHit();
                return Optional.of(card);
            }
        } catch (Exception e) {
            log.warn("Failed to read disposition from cache for key={}", key, e);
        }
        metrics.recordCacheMiss();
        return Optional.empty();
    }

    public void evict(String tenantId, long sessionId) {
        String key = buildKey(tenantId, sessionId);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to evict disposition from cache for key={}", key, e);
        }
    }

    private String buildKey(String tenantId, long sessionId) {
        return keyPrefix + ":" + tenantId + ":" + sessionId;
    }
}
