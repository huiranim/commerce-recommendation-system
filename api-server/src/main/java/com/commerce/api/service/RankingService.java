package com.commerce.api.service;

import com.commerce.api.domain.ProductStatus;
import com.commerce.api.dto.RankingResponse;
import com.commerce.api.dto.RankingResponse.RankingItem;
import com.commerce.api.repository.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RankingService {

    private static final String GLOBAL_PREFIX = "ranking:trending:";
    private static final String CATEGORY_PREFIX = "ranking:trending:category:";

    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;

    public RankingService(StringRedisTemplate redisTemplate, ProductRepository productRepository) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
    }

    public RankingResponse getTrending(String window, int limit) {
        return getByKey(GLOBAL_PREFIX + window, window, limit);
    }

    public RankingResponse getTrendingByCategory(String categoryId, String window, int limit) {
        return getByKey(CATEGORY_PREFIX + categoryId + ":" + window, window, limit);
    }

    private RankingResponse getByKey(String key, String window, int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
            redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return new RankingResponse(window, Instant.now(), List.of());
        }

        List<String> productIds = tuples.stream()
            .map(ZSetOperations.TypedTuple::getValue)
            .filter(Objects::nonNull)
            .toList();

        var productMap = productRepository
            .findAllByIdInAndStatus(productIds, ProductStatus.ACTIVE)
            .stream()
            .collect(Collectors.toMap(p -> p.getId(), Function.identity()));

        List<RankingItem> items = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String productId = tuple.getValue();
            var product = productMap.get(productId);
            if (product != null) {
                items.add(new RankingItem(rank++, productId, product.getName(),
                    product.getCategory().getId(), product.getPrice(), tuple.getScore()));
            }
        }
        return new RankingResponse(window, Instant.now(), items);
    }
}
