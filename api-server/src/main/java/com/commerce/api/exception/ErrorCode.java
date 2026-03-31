package com.commerce.api.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "Email already exists"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    EVENT_PUBLISH_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "Event publish failed"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getDefaultMessage() { return defaultMessage; }
}
