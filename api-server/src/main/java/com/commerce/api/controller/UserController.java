package com.commerce.api.controller;

import com.commerce.api.dto.UserRequest;
import com.commerce.api.dto.UserResponse;
import com.commerce.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest req) {
        return UserResponse.from(userService.create(req));
    }

    @GetMapping("/{userId}")
    public UserResponse findById(@PathVariable String userId) {
        return UserResponse.from(userService.findById(userId));
    }
}
