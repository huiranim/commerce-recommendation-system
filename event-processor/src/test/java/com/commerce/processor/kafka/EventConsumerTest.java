package com.commerce.processor.kafka;

import com.commerce.common.event.*;
import com.commerce.processor.idempotency.IdempotencyService;
import com.commerce.processor.ranking.RankingService;
import com.commerce.processor.recommendation.RecommendationService;
import com.commerce.processor.repository.ProductCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock IdempotencyService idempotencyService;
    @Mock ProductCategoryRepository productCategoryRepository;
    @Mock RankingService rankingService;
    @Mock RecommendationService recommendationService;
    @Mock DlqProducer dlqProducer;
    @InjectMocks EventConsumer eventConsumer;

    private UserBehaviorEvent viewEvent() {
        return new UserBehaviorEvent("evt-1", "1.0", "user-1", "product-1",
            EventType.VIEW, "session-1", null, Instant.now());
    }

    private UserBehaviorEvent purchaseEvent() {
        return new UserBehaviorEvent("evt-2", "1.0", "user-1", "product-1",
            EventType.PURCHASE, "session-1", null, Instant.now());
    }

    @Test
    void VIEW_이벤트_랭킹_점수_증가() {
        var event = viewEvent();
        when(idempotencyService.tryMarkProcessed("evt-1")).thenReturn(true);
        when(productCategoryRepository.getCategoryId("product-1")).thenReturn("electronics");

        eventConsumer.consume(event);

        verify(rankingService).incrementScore("product-1", "electronics", 1);
        verify(recommendationService, never()).updateUserRecommendation(any(), any(), anyDouble());
    }

    @Test
    void PURCHASE_이벤트_추천도_업데이트() {
        var event = purchaseEvent();
        when(idempotencyService.tryMarkProcessed("evt-2")).thenReturn(true);
        when(productCategoryRepository.getCategoryId("product-1")).thenReturn("electronics");
        when(rankingService.getCategoryScore("product-1", "electronics")).thenReturn(15.0);

        eventConsumer.consume(event);

        verify(rankingService).incrementScore("product-1", "electronics", 10);
        verify(recommendationService).updateUserRecommendation("user-1", "product-1", 15.0);
        verify(recommendationService).updateCategoryRecommendation("electronics", "product-1", 15.0);
    }

    @Test
    void 중복_이벤트_무시() {
        var event = viewEvent();
        when(idempotencyService.tryMarkProcessed("evt-1")).thenReturn(false);

        eventConsumer.consume(event);

        verifyNoInteractions(rankingService, recommendationService, productCategoryRepository);
    }

    @Test
    void 처리_실패시_DLQ_전송() {
        var event = viewEvent();
        when(idempotencyService.tryMarkProcessed("evt-1")).thenReturn(true);
        when(productCategoryRepository.getCategoryId("product-1")).thenThrow(new RuntimeException("DB error"));

        eventConsumer.consume(event);

        verify(idempotencyService).unmarkProcessed("evt-1");
        verify(dlqProducer).send(event);
        verifyNoInteractions(rankingService, recommendationService);
    }
}
