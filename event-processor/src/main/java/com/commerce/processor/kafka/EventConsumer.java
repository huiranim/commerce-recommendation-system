package com.commerce.processor.kafka;

import com.commerce.common.event.EventType;
import com.commerce.common.event.UserBehaviorEvent;
import com.commerce.processor.idempotency.IdempotencyService;
import com.commerce.processor.ranking.RankingService;
import com.commerce.processor.recommendation.RecommendationService;
import com.commerce.processor.repository.ProductCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final IdempotencyService idempotencyService;
    private final ProductCategoryRepository productCategoryRepository;
    private final RankingService rankingService;
    private final RecommendationService recommendationService;
    private final DlqProducer dlqProducer;

    public EventConsumer(IdempotencyService idempotencyService,
                         ProductCategoryRepository productCategoryRepository,
                         RankingService rankingService,
                         RecommendationService recommendationService,
                         DlqProducer dlqProducer) {
        this.idempotencyService = idempotencyService;
        this.productCategoryRepository = productCategoryRepository;
        this.rankingService = rankingService;
        this.recommendationService = recommendationService;
        this.dlqProducer = dlqProducer;
    }

    @KafkaListener(topics = "${kafka.topic.user-events}", groupId = "event-processor-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(UserBehaviorEvent event) {
        if (!idempotencyService.tryMarkProcessed(event.eventId())) {
            log.debug("Duplicate event skipped: {}", event.eventId());
            return;
        }
        try {
            process(event);
        } catch (Exception e) {
            log.error("Failed to process event: {}, error: {}", event.eventId(), e.getMessage(), e);
            idempotencyService.unmarkProcessed(event.eventId());
            dlqProducer.send(event);
        }
    }

    private void process(UserBehaviorEvent event) {
        String categoryId = productCategoryRepository.getCategoryId(event.productId());
        rankingService.incrementScore(event.productId(), categoryId, event.eventType().weight());

        if (event.eventType() == EventType.PURCHASE) {
            double score = rankingService.getCategoryScore(event.productId(), categoryId);
            recommendationService.updateUserRecommendation(event.userId(), event.productId(), score);
            if (categoryId != null) {
                recommendationService.updateCategoryRecommendation(categoryId, event.productId(), score);
            }
        }
    }
}
