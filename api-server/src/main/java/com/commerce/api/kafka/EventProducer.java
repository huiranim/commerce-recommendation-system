package com.commerce.api.kafka;

import com.commerce.api.exception.BusinessException;
import com.commerce.api.exception.ErrorCode;
import com.commerce.common.event.UserBehaviorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    private static final Logger log = LoggerFactory.getLogger(EventProducer.class);

    private final KafkaTemplate<String, UserBehaviorEvent> kafkaTemplate;

    @Value("${kafka.topic.user-events}")
    private String topic;

    public EventProducer(KafkaTemplate<String, UserBehaviorEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(UserBehaviorEvent event) {
        kafkaTemplate.send(topic, event.userId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send event: {}, error: {}", event.eventId(), ex.getMessage());
                    throw new BusinessException(ErrorCode.EVENT_PUBLISH_FAILED, event.eventId());
                }
                log.debug("Event sent: {}", event.eventId());
            });
    }
}
