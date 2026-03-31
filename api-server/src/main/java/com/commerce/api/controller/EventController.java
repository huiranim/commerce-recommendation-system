package com.commerce.api.controller;

import com.commerce.api.dto.EventRequest;
import com.commerce.api.dto.EventResponse;
import com.commerce.api.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventResponse publish(@Valid @RequestBody EventRequest req) {
        return eventService.publish(req);
    }
}
