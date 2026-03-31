package com.commerce.api.controller;

import com.commerce.api.dto.RecommendationResponse;
import com.commerce.api.service.RecommendationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
@Validated
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/users/{userId}")
    public RecommendationResponse getUserRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") @Positive @Max(100) int limit) {
        return recommendationService.getUserRecommendations(userId, limit);
    }

    @GetMapping("/categories/{categoryId}")
    public RecommendationResponse getCategoryRecommendations(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "10") @Positive @Max(100) int limit) {
        return recommendationService.getCategoryRecommendations(categoryId, limit);
    }
}
