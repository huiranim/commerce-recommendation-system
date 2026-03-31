package com.commerce.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationResponse(
    String userId,
    List<RecommendationItem> products
) {
    public record RecommendationItem(
        String productId,
        String name,
        String categoryId,
        BigDecimal price,
        String reason
    ) {}
}
