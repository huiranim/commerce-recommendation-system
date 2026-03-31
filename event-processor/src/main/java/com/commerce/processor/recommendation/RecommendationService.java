package com.commerce.processor.recommendation;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class RecommendationService {

    private static final String USER_PREFIX = "recommendation:user:";
    private static final String CATEGORY_PREFIX = "recommendation:category:";
    private static final Duration USER_TTL = Duration.ofHours(1);
    private static final Duration CATEGORY_TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;

    public RecommendationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateUserRecommendation(String userId, String productId, double score) {
        String key = USER_PREFIX + userId;
        redisTemplate.opsForZSet().add(key, productId, score);
        // Sliding window: TTL resets on each write so active users always have fresh recommendations
        redisTemplate.expire(key, USER_TTL);
    }

    public void updateCategoryRecommendation(String categoryId, String productId, double score) {
        String key = CATEGORY_PREFIX + categoryId;
        redisTemplate.opsForZSet().add(key, productId, score);
        // Sliding window: TTL resets on each write
        redisTemplate.expire(key, CATEGORY_TTL);
    }
}
