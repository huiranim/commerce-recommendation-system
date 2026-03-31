package com.commerce.api.dto;

import com.commerce.api.domain.Product;
import java.math.BigDecimal;

public record ProductResponse(
    String productId,
    String name,
    String categoryId,
    BigDecimal price,
    String status
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(),
            p.getCategory().getId(), p.getPrice(), p.getStatus().name());
    }
}
