package com.commerce.processor.kafka;

import com.commerce.common.event.UserBehaviorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DlqProducer {

    private static final Logger log = LoggerFactory.getLogger(DlqProducer.class);

    private final KafkaTemplate<String, UserBehaviorEvent> kafkaTemplate;

    @Value("${kafka.topic.dlq}")
    private String dlqTopic;

    public DlqProducer(KafkaTemplate<String, UserBehaviorEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(UserBehaviorEvent event) {
        log.error("Sending event to DLQ: {}", event.eventId());
        kafkaTemplate.send(dlqTopic, event.userId(), event);
    }
}
