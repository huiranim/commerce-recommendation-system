package com.commerce.api.service;

import com.commerce.api.domain.Category;
import com.commerce.api.domain.Product;
import com.commerce.api.domain.ProductStatus;
import com.commerce.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ZSetOperations<String, String> zSetOps;
    @Mock ProductRepository productRepository;
    @InjectMocks RankingService rankingService;

    @Test
    void trending_조회_상품정보_포함() {
        var category = new Category("electronics", "전자제품");
        var product = new Product("노트북", category, BigDecimal.valueOf(1200000));

        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(ZSetOperations.TypedTuple.of(product.getId(), 42.0));

        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRangeWithScores("ranking:trending:1h", 0, 19))
            .thenReturn(tuples);
        when(productRepository.findAllByIdInAndStatus(List.of(product.getId()), ProductStatus.ACTIVE))
            .thenReturn(List.of(product));

        var response = rankingService.getTrending("1h", 20);

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).rank()).isEqualTo(1);
        assertThat(response.products().get(0).score()).isEqualTo(42.0);
    }

    @Test
    void trending_Redis_비어있으면_빈_목록() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRangeWithScores(any(), anyLong(), anyLong())).thenReturn(Set.of());

        var response = rankingService.getTrending("1h", 20);

        assertThat(response.products()).isEmpty();
    }
}
