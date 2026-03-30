package com.commerce.common.event;

public enum EventType {
    VIEW, CLICK, PURCHASE;

    public int weight() {
        return switch (this) {
            case VIEW     -> 1;
            case CLICK    -> 3;
            case PURCHASE -> 10;
        };
    }
}
