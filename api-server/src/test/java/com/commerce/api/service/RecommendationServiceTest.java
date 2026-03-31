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
class RecommendationServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ZSetOperations<String, String> zSetOps;
    @Mock ProductRepository productRepository;
    @InjectMocks RecommendationService recommendationService;

    @Test
    void 유저_추천_반환() {
        var category = new Category("sports", "스포츠");
        var product = new Product("운동화", category, BigDecimal.valueOf(89000));

        Set<String> productIds = new LinkedHashSet<>(List.of(product.getId()));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRange("recommendation:user:user-1", 0, 9)).thenReturn(productIds);
        when(productRepository.findAllByIdInAndStatus(List.of(product.getId()), ProductStatus.ACTIVE))
            .thenReturn(List.of(product));

        var response = recommendationService.getUserRecommendations("user-1", 10);

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).reason()).isEqualTo("RECENT_PURCHASE_CATEGORY");
    }

    @Test
    void 유저_추천_없으면_빈_목록() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRange("recommendation:user:user-1", 0, 9)).thenReturn(Set.of());

        var response = recommendationService.getUserRecommendations("user-1", 10);

        assertThat(response.products()).isEmpty();
    }

    @Test
    void 카테고리_추천_반환() {
        var category = new Category("sports", "스포츠");
        var product = new Product("농구공", category, BigDecimal.valueOf(45000));

        Set<String> productIds = new LinkedHashSet<>(List.of(product.getId()));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.reverseRange("recommendation:category:sports", 0, 9)).thenReturn(productIds);
        when(productRepository.findAllByIdInAndStatus(List.of(product.getId()), ProductStatus.ACTIVE))
            .thenReturn(List.of(product));

        var response = recommendationService.getCategoryRecommendations("sports", 10);

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).reason()).isEqualTo("CATEGORY_POPULAR");
    }
}
