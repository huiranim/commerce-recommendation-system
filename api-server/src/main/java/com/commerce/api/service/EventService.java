package com.commerce.api.service;

import com.commerce.api.dto.EventRequest;
import com.commerce.api.dto.EventResponse;
import com.commerce.api.kafka.EventProducer;
import com.commerce.common.event.UserBehaviorEvent;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
public class EventService {

    private final EventProducer eventProducer;

    public EventService(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public EventResponse publish(EventRequest req) {
        String eventId = UUID.randomUUID().toString();
        var event = new UserBehaviorEvent(
            eventId, "1.0",
            req.userId(), req.productId(), req.eventType(),
            req.sessionId(), req.context(), Instant.now()
        );
        eventProducer.send(event);
        return new EventResponse(eventId, "ACCEPTED");
    }
}
