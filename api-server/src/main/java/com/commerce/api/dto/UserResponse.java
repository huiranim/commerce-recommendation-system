package com.commerce.api.dto;

import com.commerce.api.domain.User;

public record UserResponse(String userId, String name, String email) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail());
    }
}
