package com.commerce.processor.repository;

import jakarta.persistence.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ProductCategoryRepository {

    @PersistenceContext
    private EntityManager em;

    @Cacheable("productCategory")
    public String getCategoryId(String productId) {
        String jpql = "SELECT p.categoryId FROM ProductProjection p WHERE p.id = :id";
        try {
            return em.createQuery(jpql, String.class)
                .setParameter("id", productId)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
