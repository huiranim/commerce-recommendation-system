package com.commerce.api.repository;

import com.commerce.api.domain.Product;
import com.commerce.api.domain.ProductStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    @EntityGraph(attributePaths = "category")
    List<Product> findAllByIdInAndStatus(List<String> ids, ProductStatus status);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findWithCategoryById(String id);
}
