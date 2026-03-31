package com.commerce.processor.idempotency;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class IdempotencyService {

    private static final String KEY_PREFIX = "processed:event:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryMarkProcessed(String eventId) {
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(KEY_PREFIX + eventId, "1", TTL);
        return Boolean.TRUE.equals(result);
    }

    public void unmarkProcessed(String eventId) {
        redisTemplate.delete(KEY_PREFIX + eventId);
    }
}
