package com.commerce.api.controller;

import com.commerce.api.dto.RecommendationResponse;
import com.commerce.api.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/users/{userId}")
    public RecommendationResponse getUserRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.getUserRecommendations(userId, limit);
    }

    @GetMapping("/categories/{categoryId}")
    public RecommendationResponse getCategoryRecommendations(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.getCategoryRecommendations(categoryId, limit);
    }
}
