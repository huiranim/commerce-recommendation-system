package com.commerce.processor.ranking;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

    private static final String GLOBAL_PREFIX = "ranking:trending:";
    private static final String CATEGORY_PREFIX = "ranking:trending:category:";

    private final StringRedisTemplate redisTemplate;

    public RankingService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void incrementScore(String productId, String categoryId, int weight) {
        redisTemplate.opsForZSet().incrementScore(GLOBAL_PREFIX + "1h", productId, weight);
        redisTemplate.opsForZSet().incrementScore(GLOBAL_PREFIX + "24h", productId, weight);
        if (categoryId != null) {
            redisTemplate.opsForZSet().incrementScore(CATEGORY_PREFIX + categoryId + ":1h", productId, weight);
        }
    }

    public double getCategoryScore(String productId, String categoryId) {
        if (categoryId == null) return 0.0;
        Double score = redisTemplate.opsForZSet()
            .score(CATEGORY_PREFIX + categoryId + ":1h", productId);
        return score != null ? score : 0.0;
    }
}
