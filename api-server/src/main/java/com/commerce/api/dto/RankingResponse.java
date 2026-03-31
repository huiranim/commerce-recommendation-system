package com.commerce.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RankingResponse(
    String window,
    Instant generatedAt,
    List<RankingItem> products
) {
    public record RankingItem(
        int rank,
        String productId,
        String name,
        String categoryId,
        BigDecimal price,
        Double score
    ) {}
}
