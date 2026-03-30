package com.commerce.api.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    protected Category() {}

    /**
     * @param id   semantic string identifier (e.g. "electronics", "sports") used as a natural key
     *             and embedded in Redis ranking keys such as {@code ranking:trending:category:{id}:1h}
     * @param name human-readable display name
     */
    public Category(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Category getParent() { return parent; }
}
