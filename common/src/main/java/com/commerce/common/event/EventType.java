package com.commerce.common.event;

public enum EventType {
    VIEW(1), CLICK(3), PURCHASE(10);

    private final int weight;

    EventType(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
