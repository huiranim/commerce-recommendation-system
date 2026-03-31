package com.commerce.processor.ranking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ZSetOperations<String, String> zSetOps;
    @InjectMocks RankingService rankingService;

    @Test
    void incrementScore_세_개_키에_점수_추가() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        rankingService.incrementScore("product-1", "electronics", 10);

        verify(zSetOps).incrementScore("ranking:trending:1h", "product-1", 10.0);
        verify(zSetOps).incrementScore("ranking:trending:24h", "product-1", 10.0);
        verify(zSetOps).incrementScore("ranking:trending:category:electronics:1h", "product-1", 10.0);
    }

    @Test
    void getCategoryScore_Redis_점수_반환() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.score("ranking:trending:category:electronics:1h", "product-1")).thenReturn(42.0);

        double score = rankingService.getCategoryScore("product-1", "electronics");

        assertThat(score).isEqualTo(42.0);
    }
}
