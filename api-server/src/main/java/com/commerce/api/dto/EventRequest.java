package com.commerce.api.dto;

import com.commerce.common.event.EventContext;
import com.commerce.common.event.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventRequest(
    @NotBlank String userId,
    @NotBlank String productId,
    @NotNull EventType eventType,
    String sessionId,
    EventContext context
) {}
