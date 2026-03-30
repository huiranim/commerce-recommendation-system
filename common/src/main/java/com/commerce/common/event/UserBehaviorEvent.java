package com.commerce.common.event;

import java.time.Instant;

public record UserBehaviorEvent(
    String eventId,
    String schemaVersion,
    String userId,
    String productId,
    EventType eventType,
    String sessionId,
    EventContext context,
    Instant timestamp
) {}
