package com.commerce.api.service;

import com.commerce.api.domain.ProductStatus;
import com.commerce.api.dto.RecommendationResponse;
import com.commerce.api.dto.RecommendationResponse.RecommendationItem;
import com.commerce.api.repository.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final String USER_PREFIX = "recommendation:user:";
    private static final String CATEGORY_PREFIX = "recommendation:category:";

    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;

    public RecommendationService(StringRedisTemplate redisTemplate, ProductRepository productRepository) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
    }

    public RecommendationResponse getUserRecommendations(String userId, int limit) {
        Set<String> productIds = redisTemplate.opsForZSet()
            .reverseRange(USER_PREFIX + userId, 0, limit - 1);
        return buildResponse(userId, productIds, "RECENT_PURCHASE_CATEGORY");
    }

    public RecommendationResponse getCategoryRecommendations(String categoryId, int limit) {
        Set<String> productIds = redisTemplate.opsForZSet()
            .reverseRange(CATEGORY_PREFIX + categoryId, 0, limit - 1);
        return buildResponse(null, productIds, "CATEGORY_POPULAR");
    }

    private RecommendationResponse buildResponse(String userId, Set<String> productIds, String reason) {
        if (productIds == null || productIds.isEmpty()) {
            return new RecommendationResponse(userId, List.of());
        }
        var productMap = productRepository
            .findAllByIdInAndStatus(new ArrayList<>(productIds), ProductStatus.ACTIVE)
            .stream()
            .collect(Collectors.toMap(p -> p.getId(), Function.identity()));

        List<RecommendationItem> items = productIds.stream()
            .map(id -> {
                var product = productMap.get(id);
                if (product == null) return null;
                return new RecommendationItem(id, product.getName(),
                    product.getCategory().getId(), product.getPrice(), reason);
            })
            .filter(Objects::nonNull)
            .toList();

        return new RecommendationResponse(userId, items);
    }
}
