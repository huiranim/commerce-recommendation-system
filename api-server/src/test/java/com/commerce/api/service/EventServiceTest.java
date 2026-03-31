package com.commerce.api.service;

import com.commerce.api.dto.EventRequest;
import com.commerce.api.kafka.EventProducer;
import com.commerce.common.event.EventType;
import com.commerce.common.event.UserBehaviorEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventProducer eventProducer;
    @InjectMocks EventService eventService;

    @Test
    void publish_eventId_반환() {
        var req = new EventRequest("user-1", "product-1", EventType.VIEW, "session-1", null);
        doNothing().when(eventProducer).send(any());

        var response = eventService.publish(req);

        assertThat(response.eventId()).isNotBlank();
        assertThat(response.status()).isEqualTo("ACCEPTED");
    }

    @Test
    void publish_이벤트_내용_검증() {
        var req = new EventRequest("user-1", "product-1", EventType.PURCHASE, "session-1", null);
        var captor = ArgumentCaptor.forClass(UserBehaviorEvent.class);
        doNothing().when(eventProducer).send(captor.capture());

        eventService.publish(req);

        var captured = captor.getValue();
        assertThat(captured.userId()).isEqualTo("user-1");
        assertThat(captured.productId()).isEqualTo("product-1");
        assertThat(captured.eventType()).isEqualTo(EventType.PURCHASE);
        assertThat(captured.schemaVersion()).isEqualTo("1.0");
    }
}
