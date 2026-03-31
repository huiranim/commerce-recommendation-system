package com.commerce.api.controller;

import com.commerce.api.dto.RankingResponse;
import com.commerce.api.service.RankingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/trending")
    public RankingResponse getTrending(
            @RequestParam(defaultValue = "1h") String window,
            @RequestParam(defaultValue = "20") int limit) {
        return rankingService.getTrending(window, limit);
    }

    @GetMapping("/trending/categories/{categoryId}")
    public RankingResponse getTrendingByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "1h") String window,
            @RequestParam(defaultValue = "20") int limit) {
        return rankingService.getTrendingByCategory(categoryId, window, limit);
    }
}
