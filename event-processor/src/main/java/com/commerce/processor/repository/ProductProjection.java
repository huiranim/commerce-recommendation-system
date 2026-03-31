package com.commerce.processor.repository;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class ProductProjection {

    @Id
    private String id;

    @Column(name = "category_id")
    private String categoryId;

    protected ProductProjection() {}

    public String getId() { return id; }
    public String getCategoryId() { return categoryId; }
}
